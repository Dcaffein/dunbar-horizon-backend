# PLAN.md — Task-83: Friendship 감쇄 버그 수정 및 코드 정비

## 작업 목표

`applyDecay` Cypher 쿼리의 collect 버그로 `f.intimacy = 0.0`이 잘못 설정되어 활성 관계가 삭제되는 문제를 수정한다.
아울러 버그의 기원이 된 Java dead branch 제거, `Friendship.id` 인덱스 추가, Repository 쿼리 정비를 함께 진행한다.

---

## 현황 분석

### 변경 대상 파일

| 파일 | 현재 상태 |
|------|----------|
| `Friendship.java` | `recalculateIntimacy`에 dead branch + `calculateIntimacyPolicy` 존재 |
| `FriendshipNeo4jRepository.java` | `applyDecay` 버그. 일부 쿼리 derived로 대체 가능 |

### `FriendshipNeo4jRepository` 쿼리 분류

**inherited 메서드로 대체 가능 (derived):**

| 현재 | 대체 |
|------|------|
| `findById` override | 제거 — `Neo4jRepository.findById` 그대로 사용 |
| `findAllByIds(Collection<String>)` @Query | `findAllById(Iterable<String>)` 상속 메서드 사용 |
| `deleteAllByIdIn(Collection<String>)` @Query | `deleteAllById(Iterable<String>)` 상속 메서드 사용 |

**@Query 유지 필요 (UserReference 경유 탐색 또는 관계 프로퍼티 조건):**

`findFriendshipsByUserId`, `findFriendIdsIn`, `findFriendIds`, `findFriendIdsByMuteStatus`, `findFriendshipsByMuteStatus`, `findFriendshipsIn`, `applyDecay`, `batchUpdateInterestScores`, `updateUserRelationshipFields`

SDN6 derived query는 `Neo4jRepository<Friendship, String>` 기준으로 `Friendship` 자신의 프로퍼티만 조건으로 쓸 수 있다. UserReference 경유 탐색이나 관계 프로퍼티(`isMuted` 등) 조건은 표현 불가하므로 @Query를 유지한다.

---

## 변경 파일 목록

### 1. `social/domain/friend/Friendship.java`

- `recalculateIntimacy()`: dead branch(`size < 2`) 제거, `calculateIntimacyPolicy()` 제거 후 `Math.sqrt` 인라인

### 2. `social/adapter/out/persistence/neo4j/springData/FriendshipNeo4jRepository.java`

- `findById` override 제거
- `findAllByIds` @Query → `findAllById` 상속 메서드로 대체 (시그니처 변경)
- `deleteAllByIdIn` @Query → `deleteAllById` 상속 메서드로 대체 (시그니처 변경)
- `applyDecay` 쿼리 수정:
  - 감쇄 후 `WITH DISTINCT f`로 영향받은 Friendship만 추출
  - 전체 엣지 재조회: `MATCH (all_u:UserReference)-[all_r:HAS_FRIENDSHIP]->(f)`
  - `collect(all_r.interestScore)`로 항상 2개 수집 보장
  - `ELSE 0.0` 분기 제거

### 3. `social/adapter/out/persistence/neo4j/FriendshipRepositoryAdapter.java`

- `findAllByIds` → `findAllById` 시그니처 변경에 따른 어댑터 수정
- `deleteAllByIds` → `deleteAllById` 시그니처 변경에 따른 어댑터 수정

---

## 구현 방향

- 감쇄 조건(`r.lastInteractedAt`, `r.interestScore > $threshold`) 및 이관 조건은 변경하지 않는다. 버그는 collect 범위 문제이며, 판정 로직 자체는 정상이다.
- `findAllById` / `deleteAllById`는 SDN6가 `@Relationship(INCOMING)`을 자동으로 로드하므로 현재 @Query와 동일한 결과를 보장한다.
- `FriendshipRepository` 포트 인터페이스의 메서드 시그니처도 어댑터 변경에 맞게 수정한다.

---

## 예상 사이드 이펙트

- `findAllByIds` → `findAllById` 시그니처 변경으로 `InteractionScoreFlushService`의 호출부 수정 필요
- `deleteAllByIds` → `deleteAllById` 시그니처 변경으로 `FriendshipArchiveService`의 호출부 수정 필요

---

## 테스트 전략

기본 프로토콜(`TESTING-GUIDE.md`)을 따른다.
`FriendshipDecayServiceTest` — `applyDecay` 파라미터 전달 단위 검증.
