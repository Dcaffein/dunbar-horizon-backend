# PLAN — Task-85: findByUserId 쿼리 비활성 유저 미처리 수정

## 작업 목표

`FriendshipNeo4jRepository.findByUserId` 의 쿼리가 비활성 친구가 있을 때 `MappingException`
을 던지는 버그를 수정하고, 다른 쿼리 메서드들의 동일 문제 여부를 검토한다.

---

## 현황 분석

### 수정 대상: `findByUserId`

```cypher
-- 현재 (버그)
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
MATCH (f)<-[all_r:HAS_FRIENDSHIP]-(all_u:UserReference)  -- 비활성 친구 누락
RETURN f, collect(all_r), collect(all_u)
```

두 번째 MATCH가 `UserReference` 레이블로 필터하므로, 친구가 비활성(`InactiveSocialUser`)이면
해당 관계가 `collect(all_r)` 에서 빠져 `recognitions.size() == 1` → `FriendshipInvalidException`.

### 다른 쿼리 메서드 검토 결과

| 메서드 | `UserReference` 사용 방식 | 비활성 유저 시 동작 | 판정 |
|--------|--------------------------|-------------------|------|
| `findFriendIdsByMuteStatus` | `(friend:UserReference)` | 비활성 친구 ID 누락 | 묵음 목록에서 제외 — 비즈니스상 허용 |
| `filterFriendIdsAmong` | `(friend:UserReference)` | 비활성 친구 필터 탈락 | 친구 여부 체크에서 false 반환 — 허용 |
| `applyDecay` | `(u:UserReference)` | 비활성 유저 엣지 감쇄 스킵 | 비활성 유저의 interestScore 미감쇄 — 허용 |
| `batchUpdateInterestScores` | `(:UserReference)` | 비활성 유저 업데이트 스킵 | 동일 — 허용 |
| `updateUserRelationshipFields` | `(:UserReference)` | 비활성 유저면 SET 무시 | 비활성 유저 관계 필드 수정 불가 — 허용 |

`findByUserId` 외 다른 메서드들은 엔티티 매핑 없이 ID 반환 또는 순수 SET 쿼리이므로,
비활성 유저를 제외하는 것이 예외가 아닌 자연스러운 동작이다. **추가 수정 불필요**.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `social/adapter/out/persistence/neo4j/springData/FriendshipNeo4jRepository.java` | `findByUserId` `@Query` 수정 |
| `social/adapter/out/FriendshipNeo4jRepositoryTest.java` | 비활성 유저 시나리오 테스트 케이스 추가 |

---

## 구현 방향

### `findByUserId` 쿼리 (두 단계 분리)

```cypher
-- 1단계: 익명 (:UserReference) 로 친구 활성 여부만 검증, f만 추출
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
      <-[:HAS_FRIENDSHIP]-(:UserReference)

-- 2단계: 걸러진 f에 대해 레이블 제약 없이 양쪽 관계 수집
MATCH (f)<-[all_r:HAS_FRIENDSHIP]-(all_u)
RETURN f, collect(all_r), collect(all_u)
```

- 1단계의 익명 `(:UserReference)` 는 Cypher 경로 내 관계 유일성 규칙에 의해
  반드시 `$userId` 본인이 아닌 상대방으로 매칭됨
- 상대방이 비활성이면 1단계에서 f가 결과에서 제외됨 (비즈니스 요구 충족)
- 2단계에서 레이블 제약 없이 수집하므로 `recognitions` 항상 2개 보장

### `FriendshipNeo4jRepositoryTest` 추가 케이스

기존 테스트들은 모두 활성 유저끼리만 검증하므로 수정 불필요.
새 쿼리에서도 양쪽이 활성이면 동작이 동일하기 때문이다.

`UserReference` 레이블을 사용하는 모든 쿼리에 대해 비활성 유저 시나리오를 추가한다.
코드를 수정하지 않더라도 현재 동작을 테스트로 명시하고, 의도와 다를 경우 수정한다.

| 메서드 | 추가 케이스 |
|--------|-----------|
| `findByUserId` | 비활성 친구 → 결과에서 제외되는지 |
| `filterFriendIdsAmong` | 비활성 친구 → `false` 반환인지 (친구 여부 체크 영향) |
| `findFriendIdsByMuteStatus` | 비활성 친구를 묵음해도 목록에서 제외되는지 |
| `updateUserRelationshipFields` | 비활성 유저 본인의 관계 필드 수정 불가 여부 |

---

## 예상 사이드 이펙트

없음. 변경 범위가 단일 `@Query` 수정에 한정되며, 비즈니스 동작은 "비활성 친구 제외" 로 명확히 정의된다.

---

## 테스트 전략

`Neo4jRepositoryTest` 기반으로 작성한다. `SocialUser.deactivate()` 를 호출하거나
`@DynamicLabels` 를 직접 조작하여 비활성 노드를 만든 뒤 `findByUserId` 결과를 검증한다.
