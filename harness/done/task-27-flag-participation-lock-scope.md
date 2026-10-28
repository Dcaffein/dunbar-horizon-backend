# Task 27: Flag 참여 시 비관적 락 보유 중 Neo4j 조회 제거

## Objective
`FlagParticipationPolicy.participate()`에서 MySQL 비관적 락을 보유한 상태로 Neo4j 친구 확인 쿼리가 실행되는 문제를 해결한다.
친구 확인을 락 획득 전에 수행해 락 보유 시간을 최소화한다.

## Background

### 현재 호출 흐름

```
FlagParticipationService.participateInFlag()
  ├─ flagRepository.findByIdExclusive(flagId)
  │    └─ SELECT ... FOR UPDATE  ← MySQL 비관적 락 획득
  │
  └─ flagParticipationPolicy.participate(flag, userId)   ← 락 보유 중
       ├─ isParticipating()          → MySQL 쿼리 (FlagParticipant)
       ├─ areFriends()               → Neo4j 쿼리  ← 문제 지점
       │    FlagUserAdapter.areFriends()
       │      → FriendshipQueryService.areFriends()
       │          → friendshipRepository.existsFriendshipBetween()  (Neo4j)
       ├─ countByFlagId()            → MySQL 쿼리 (FlagParticipant)
       └─ flag.participate()         → FlagParticipant 생성
  └─ participantRepository.save()
```

### 문제
`findByIdExclusive()`로 MySQL Flag 행에 `PESSIMISTIC_WRITE` 락을 획득한 상태에서
`areFriends()`가 **Neo4j** 쿼리를 실행한다.

- MySQL 락은 Neo4j 응답이 돌아올 때까지 유지된다.
- Neo4j 레이턴시(네트워크 + 그래프 탐색)만큼 락 보유 시간이 늘어난다.
- 친구 확인은 **정원 체크나 중복 체크와 달리 락이 필요한 임계 영역이 아니다.**
  - "지금 이 순간 친구인가"는 락과 무관하게 미리 확인해도 정합성에 영향 없다.

## Decision

### 변경 방향
`areFriends()` 호출을 `FlagParticipationService`로 올려서 락 획득 전에 수행한다.
`FlagParticipationPolicy`는 락 임계 영역 안에서 필요한 검사만 담당하도록 좁힌다.

**변경 후 흐름:**
```
FlagParticipationService.participateInFlag()
  ├─ friendshipChecker.areFriends()   ← 락 획득 전 (Neo4j 쿼리)
  │    └─ 친구가 아니면 즉시 FlagAuthorizationException
  │
  ├─ flagRepository.findByIdExclusive(flagId)
  │    └─ SELECT ... FOR UPDATE  ← MySQL 비관적 락 획득
  │
  └─ flagParticipationPolicy.participate(flag, userId)
       ├─ isParticipating()    → MySQL 쿼리 (중복 체크, 락 임계 영역)
       ├─ countByFlagId()      → MySQL 쿼리 (정원 체크, 락 임계 영역)
       └─ flag.participate()   → FlagParticipant 생성
  └─ participantRepository.save()
```

### 체크리스트
- [x] `FlagParticipationPolicy`가 `FlagRepository`를 직접 보유해 전체 참여 흐름 담당
- [x] `participate(Flag, userId)` → `participate(Long flagId, userId)` 시그니처 변경
- [x] `validateParticipationEligibility` 해체 후 순서 재정렬
- [x] `FlagParticipationService` 단순화 (flag 독점 조회 제거, policy 단순 위임)
- [x] 테스트 수정 및 신규 PolicyTest 작성

---

## Result

**브랜치:** `ai/refactor-flag-participation-lock-scope`
**커밋:** `d8e5048`

### 최종 구현 흐름

초기 계획(Service에서 areFriends 호출)에서 논의를 거쳐 Policy가 전체 흐름을 책임지는 방향으로 변경.
`validateParticipationEligibility`는 재사용되지 않아 해체하고 로직을 인라인.

```
FlagParticipationPolicy.participate(flagId, userId)
  ├─ flagRepository.findById()         → 비독점 조회 (hostId 확보)
  ├─ friendshipChecker.areFriends()    → Neo4j (락 전)
  ├─ flagRepository.findByIdExclusive() → MySQL 비관적 락 획득
  ├─ isParticipating()                 → MySQL (중복 체크, 락 임계 영역)
  ├─ countByFlagId()                   → MySQL (정원 체크, 락 임계 영역)
  └─ flag.participate()                → FlagParticipant 생성

FlagParticipationService.participateInFlag()
  ├─ policy.participate(flagId, userId)
  └─ participantRepository.save()
```

### 변경 내용

#### `FlagParticipationPolicy`
- `FlagRepository` 의존성 추가
- `participate(Flag flag, Long userId)` → `participate(Long flagId, Long userId)`
- `validateParticipationEligibility` 메서드 제거, 순서 재정렬 인라인 처리

#### `FlagParticipationService`
- `flagRepository.findByIdExclusive` 호출 제거
- `policy.participate(flagId, userId)` + `participantRepository.save()` 만 담당

### 테스트 결과
- `FlagParticipationServiceTest`: 4/4 PASSED (정상 참여, FlagNotFoundException 전파, DuplicateException 전파, leaveFlag 2건)
- `FlagParticipationPolicyTest` (신규): 4/4 PASSED (정상 참여, 플래그 없음, 비친구, 중복 참여)
