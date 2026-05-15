# PLAN: Task-27 — Flag 참여 시 비관적 락 보유 중 Neo4j 조회 제거

## 작업 목표

`FlagParticipationPolicy.validateParticipationEligibility()`의 `areFriends()` (Neo4j 쿼리)를
MySQL 비관적 락 획득 전에 실행하도록 호출 순서를 변경한다.
락 보유 시간을 MySQL 임계 영역(중복 체크·정원 체크·참여자 생성)으로만 제한한다.

---

## 현황 분석

**현재 흐름 (문제):**
```
Service.participateInFlag()
  └─ flagRepository.findByIdExclusive()  ← MySQL PESSIMISTIC_WRITE 락 획득
  └─ policy.participate(flag, userId)    ← 락 보유 중
       └─ validateParticipationEligibility()
            ├─ isParticipating()  → MySQL (OK)
            └─ areFriends()       → Neo4j ← 락 보유 중 네트워크+그래프 탐색 발생
       └─ countByFlagId()         → MySQL (OK)
       └─ flag.participate()
```

**문제:** `areFriends()`는 정원·중복과 달리 락 없이도 정합성에 영향 없다.
Neo4j 레이턴시만큼 MySQL 락 보유 시간이 늘어나 다른 참여 요청을 불필요하게 대기시킨다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `flag/domain/flag/FlagParticipationPolicy.java` | `FlagRepository` 의존성 추가, `participate` 시그니처 변경 (`Flag` → `Long flagId`), `validateParticipationEligibility` 해체 후 순서 재정렬 |
| `flag/application/service/flag/FlagParticipationService.java` | flag 독점 조회 제거, `policy.participate(flagId, userId)` 호출로 단순화 |
| `flag/application/service/flag/FlagParticipationServiceTest.java` | flag 독점 조회 stub 제거, policy mock으로 단순화 |
| `flag/domain/flag/FlagParticipationPolicyTest.java` (신규) | 순서 재정렬된 Policy 비즈니스 로직 단위 테스트 |

---

## 구현 방향

### 1. `FlagParticipationPolicy` 변경

`validateParticipationEligibility`를 해체하고, Policy가 `FlagRepository`를 직접 보유하여 전체 흐름을 책임진다.

**변경 후 흐름:**
```java
// FlagParticipationPolicy.participate(Long flagId, Long userId)

// 1. 비독점 조회 — hostId 확보 (블로킹 없음)
Flag flagForCheck = flagRepository.findById(flagId)
        .orElseThrow(() -> new FlagNotFoundException(flagId));

// 2. 친구 확인 (Neo4j, 락 전)
if (!friendshipChecker.areFriends(flagForCheck.getHostId(), userId)) {
    throw new FlagAuthorizationException("호스트의 친구만 참여할 수 있는 플래그입니다.");
}

// 3. MySQL 비관적 락 획득
Flag flag = flagRepository.findByIdExclusive(flagId)
        .orElseThrow(() -> new FlagNotFoundException(flagId));

// 4. 중복 참여 체크 (락 임계 영역)
if (flagParticipantRepository.isParticipating(flagId, userId)) {
    throw new FlagParticipationDuplicateException(flagId, userId);
}

// 5. 정원 체크 및 참여자 생성
int currentCount = flagParticipantRepository.countByFlagId(flagId);
return flag.participate(userId, currentCount);
```

### 2. `FlagParticipationService` 단순화

```java
@Override
public void participateInFlag(Long flagId, Long userId) {
    FlagParticipant newParticipant = flagParticipationPolicy.participate(flagId, userId);
    participantRepository.save(newParticipant);
}
```

---

## 테스트 전략

### `FlagParticipationServiceTest` 단순화
- `policy.participate(flagId, userId)` 호출 및 `participantRepository.save()` 호출 검증만 수행

### `FlagParticipationPolicyTest` 신규
1. 정상 참여 — areFriends(true), isParticipating(false) stub 후 `participate` 반환 검증
2. 비친구 — areFriends(false) → `FlagAuthorizationException`
3. 중복 참여 — isParticipating(true) → `FlagParticipationDuplicateException`
