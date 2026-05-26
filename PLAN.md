# PLAN: FlagParticipationPolicy 1PC 캐시 오염 수정

## 작업 목표

`FlagParticipationPolicy.participate()`의 첫 번째 `findById` 호출이 `Flag` 엔티티 전체를 1PC에 로드함으로써,
이후 `findByIdExclusive`가 stale entity를 반환할 수 있는 문제를 제거한다.

## 현황 분석

`participate()` 흐름:
1. `flagRepository.findById(flagId)` — 엔티티 전체 로드 → 1PC 오염
2. `friendshipChecker.areFriends(hostId, userId)` — Neo4j 조회
3. `flagRepository.findByIdExclusive(flagId)` — JPQL + PESSIMISTIC_WRITE

Hibernate는 1PC에 이미 로드된 엔티티가 있으면 `findByIdExclusive`의 DB 결과를 버리고 캐시된 인스턴스를 반환한다.
2번 단계 사이에 호스트가 `closeRecruitment()`를 완료하면 `lockedFlag.isRecruiting()`이 stale `deadline`을 읽어
모집 마감된 플래그에 참여가 허용되는 버그가 발생할 수 있다.

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `flag/domain/flag/repository/FlagRepository.java` | `findHostIdById(Long id): Optional<Long>` 추가 |
| `flag/adapter/out/persistence/jpa/FlagJpaRepository.java` | `hostId` 스칼라 projection 쿼리 추가 |
| `flag/adapter/out/persistence/FlagRepositoryAdapter.java` | 포트 구현 추가 |
| `flag/domain/flag/FlagParticipationPolicy.java` | `findById` → `findHostIdById` 교체 |

## 구현 방향

`findById` 대신 `hostId` 스칼라만 조회하여 1PC에 `Flag` 엔티티가 로드되지 않게 한다.
이후 `findByIdExclusive` 호출 시 1PC miss → Hibernate가 DB에서 fresh entity를 로드한다.

```java
// Before
Flag flag = flagRepository.findById(flagId).orElseThrow(...);
friendshipChecker.areFriends(flag.getHostId(), userId);

// After
Long hostId = flagRepository.findHostIdById(flagId).orElseThrow(...);
friendshipChecker.areFriends(hostId, userId);
```

## 예상 사이드 이펙트

없음. 첫 번째 조회의 목적이 오직 `hostId` 확보였으므로 동작 변경 없음.

## Result

- 브랜치: `ai/fix-flag-participation-policy-1pc`
- 변경 파일:
  - `flag/domain/flag/repository/FlagRepository.java` — `findHostIdById` 추가
  - `flag/adapter/out/persistence/jpa/FlagJpaRepository.java` — JPQL 스칼라 projection 쿼리 추가
  - `flag/adapter/out/persistence/FlagRepositoryAdapter.java` — 포트 구현 추가
  - `flag/domain/flag/FlagParticipationPolicy.java` — `findById` → `findHostIdById` 교체
