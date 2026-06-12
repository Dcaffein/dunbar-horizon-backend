# PLAN: Task-65 — currentSkeletonIds 보안 검증 및 외부인 엣지 탐색 제한

## Branch
`ai/feat-secure-skeleton-ids` (from `main`)

## 목표

`/mutual/one-hop`, `/mutual/two-hop`의 skeleton 계산 방식을 클라이언트 전달(`currentSkeletonIds`)로 전환하면서
보안 검증과 엣지 탐색 제한을 동시에 적용한다.

---

## 현황 분석

### 현재 구조
- `buildCurrentSkeleton(userId, labelId)`이 매 호출마다 DB를 재조회해 skeleton을 서버에서 계산
- 클라이언트가 드래그앤드롭으로 화면을 확장해도 서버는 초기 `labelId` + `circleSize` 기준 skeleton을 계속 사용
- `getNewNodeEdges`: LIMIT 없음 — skeleton에 속하는 target 친구 엣지를 전부 반환
- `getNetworkContactsOfTwoHop`: 하드코딩 `LIMIT 5`

### 보안 취약점
`currentSkeletonIds`를 검증 없이 사용하면 임의 ID로 `target`의 친구 관계를 열거할 수 있다.
방어: `FriendshipRepository.findFriendIdsIn(userId, skeletonIds)`로 내 실제 친구 ID와 교집합 후 사용.

### dynamicLimit
- `buildDynamicPruningNetwork` 기존 공식: `5 + intimacy * 25` → 5~30
- 외부인 skeleton 노드 적용 공식: `5 + intimacy * 5` → 5~10
- skeleton 노드들의 me→mutual 친밀도 중 `MAX`를 취해 전체 결과의 global limit으로 사용
  - 친밀도 낮은 외부인만 있으면 최대 5개, 친밀도 높은 경우 최대 10개 노출

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `social/adapter/in/web/SocialQueryController.java` | `labelId`, `circleSize` 파라미터 → `skeletonIds: List<Long>` |
| `social/application/port/in/SocialNetworkQueryUseCase.java` | 두 메서드 시그니처에서 `labelId`, `limitSize` → `List<Long> skeletonIds` |
| `social/application/service/SocialNetworkQueryService.java` | `FriendshipRepository` 주입. `skeletonIds` 빈 리스트 early return + me→target 친밀도 PK 조회 후 `dynamicLimit` 계산 |
| `social/application/port/out/SocialNetworkRepository.java` | `getNewNodeEdges`에 `dynamicLimit` 파라미터 추가, `getNetworkContactsOfTwoHop` 시그니처 변경 |
| `social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java` | `buildCurrentSkeleton` 제거. 단일 Neo4j 쿼리로 보안 검증 + dynamicLimit 계산 + 조기 종료 |

---

## 구현 방향

### Controller
```java
@GetMapping("/mutual/one-hop")
public ResponseEntity<...> getOneHopMutualFriendEdges(
    @CurrentUserId Long currentUserId,
    @RequestParam Long targetId,
    @RequestParam List<Long> skeletonIds
) { ... }
```

### Service
```java
// getNewNodeEdges
public List<MutualFriendEdgeResult> getNewNodeEdges(
        Long userId, Long targetId, List<Long> skeletonIds) {
    if (skeletonIds == null || skeletonIds.isEmpty()) return List.of();
    double intimacy = friendshipRepository
            .findById(Friendship.generateCompositeId(userId, targetId))
            .map(Friendship::getIntimacy).orElse(0.0);
    int dynamicLimit = (int)(5 + intimacy * 5);
    return socialNetworkRepository.getNewNodeEdges(userId, targetId, skeletonIds, dynamicLimit);
}

// getNetworkContactsOfTwoHop — target이 아직 내 친구가 아니므로 intimacy 조회 없음
public List<NetworkOneHopsByTwoHopResult> getNetworkContactsOfTwoHop(
        Long userId, Long targetId, List<Long> skeletonIds) {
    if (skeletonIds == null || skeletonIds.isEmpty()) return List.of();
    return socialNetworkRepository.getNetworkContactsOfTwoHop(userId, targetId, skeletonIds);
}
```

보안 검증은 Neo4j 쿼리의 HAS_FRIENDSHIP 패턴 매칭이 담당한다.
`(me)-[:HAS_FRIENDSHIP]->(:FRIENDSHIP)<-[:HAS_FRIENDSHIP]-(mutual) WHERE mutual.id IN $skeletonIds`
→ 실제 친구 관계가 없는 임의 ID는 패턴 매칭 단계에서 자연스럽게 제거된다.

### Repository — `getNewNodeEdges` Cypher 구조

target은 이미 내 1-hop 친구이므로 신뢰 관계 성립 → isRoutable 불필요.
dynamicLimit은 서비스에서 계산해 `$dynamicLimit` 파라미터로 전달 → CALL 서브쿼리 안에서 LIMIT에 사용 가능.

`MATCH (me), (target)` 콤마 분리는 카테시안 프로덕트를 유발하므로 `WITH me` 를 사이에 두어 순차 MATCH로 분리.
`CALL { WITH me, target ... }` 은 deprecated → `CALL (me, target) { ... }` 사용.
노드 라벨: `UserReference` (USER_REFERENCE), `Friendship` (FRIENDSHIP) 명시.

```cypher
MATCH (me:UserReference {id: $meId})
WITH me
MATCH (target:UserReference {id: $targetId})
CALL (me, target) {
  MATCH (me)-[:HAS_FRIENDSHIP]->(:Friendship)<-[:HAS_FRIENDSHIP]-(mutual:UserReference)
  WHERE mutual.id IN $skeletonIds
  MATCH (target)-[:HAS_FRIENDSHIP]->(tf:Friendship)<-[:HAS_FRIENDSHIP]-(mutual)
  ORDER BY tf.intimacy DESC
  LIMIT $dynamicLimit
  RETURN mutual, tf
}
RETURN target.id AS friendAId, mutual.id AS friendBId, tf.intimacy AS intimacy
```

> **필터 위치 근거**: `WHERE mutual.id IN $skeletonIds`를 첫 번째 MATCH 직후에 배치하면, 플래너가 NodeHashJoin Build 단계에서 me의 친구 전체 대신 skeletonIds에 속한 친구만 해시 테이블에 올린다. skeletonIds는 통상 5~15개로 작기 때문에 Build 비용을 크게 줄인다.

### Repository — `getNetworkContactsOfTwoHop` Cypher 구조

target은 이미 추천으로 화면에 표시된 2-hop 유저이므로, 화면에 이미 올라온 노드 간의 접점을 찾는 용도다.
isRoutable은 네트워크 탐색 시 새로운 사람을 노출할지 결정하는 설정이므로, 이 쿼리에는 적용하지 않는다.
(isRoutable은 expansion 계열 쿼리에서 사용하는 것이 의미상 맞다.)

isRoutable 제거로 `getNewNodeEdges`와 구조가 동일해져 플래너가 힌트 없이 NodeHashJoin을 선택한다.

```cypher
MATCH (me:UserReference {id: $meId})
WITH me
MATCH (target:UserReference {id: $targetId})
CALL (me, target) {
  MATCH (me)-[:HAS_FRIENDSHIP]->(:Friendship)<-[:HAS_FRIENDSHIP]-(mutual:UserReference)
  WHERE mutual.id IN $skeletonIds
  MATCH (target)-[:HAS_FRIENDSHIP]->(tf:Friendship)<-[:HAS_FRIENDSHIP]-(mutual)
  ORDER BY tf.intimacy DESC
  LIMIT 5
  RETURN mutual, tf
}
RETURN mutual.id AS friendId
```

---

## 예상 사이드 이펙트

- **API 파라미터 변경**: 클라이언트가 `labelId` + `circleSize` 대신 `skeletonIds` 배열을 전달해야 함
- `SocialNetworkQueryService`에서 `FriendshipRepository` 의존성 불필요
- `getNetworkContactsOfTwoHop`의 `limitSize` 바인딩 제거

---

## 테스트 전략

- `SocialNetworkQueryServiceTest`:
  - `skeletonIds` 빈 리스트 early return 검증
  - me→target 친밀도에 따라 dynamicLimit(5~10)이 올바르게 계산되어 repository에 전달되는지
  - target과 친구 관계가 없을 때 intimacy=0.0 fallback → dynamicLimit=5
- Repository 통합 테스트:
  - 비친구 ID가 포함된 skeletonIds 전달 시 결과에서 제외되는지
  - `$dynamicLimit`만큼만 결과가 반환되는지
