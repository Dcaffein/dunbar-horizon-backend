# Task 66: 네트워크 응답을 per-node 구조로 교체

## Status: 완료

## 문제

`GET /api/v1/networks/me`는 친구 간 엣지 목록만 반환했다. 프론트엔드는 이 엣지를 기반으로 노드를 추론해 렌더링하기 때문에, circleSize 내에 있어도 다른 친구와 연결이 없는 **고립 노드**는 화면에 표시되지 않았다.

## 결정: 응답 구조를 per-node로 교체

초기에는 별도 엔드포인트(`GET /me/nodes`) 추가나 Facade 패턴(두 쿼리 조합)을 검토했다.
최종 결정은 **Cypher RETURN 구조 자체를 변경**하는 것이다.

기존 `UNWIND topEdges AS edgeData` → per-edge row 방식 대신,  
`topEdges`를 list comprehension으로 처리해 **한 행 = 한 친구**로 반환한다.  
고립 노드는 `memberEdges = []`인 행으로 자연스럽게 포함된다.

## 변경 내용

### DTO 교체

| 삭제 | 추가 |
|------|------|
| `NetworkFriendEdgeResult` | `NodeGraphResult(nodeId, interestScore, edges)` |
| `NetworkGraphResult(nodeIds, edges)` | `NetworkGraphResult(nodes)` |
| | `NodeEdgeResult(friendId, intimacy, friendInterest)` |

### 쿼리 구조 변경

`NETWORK_PRUNING_SUFFIX` (공통 suffix)의 `[3]`, `[4]` 변경:

```cypher
-- [3] CALL: 노드 객체 대신 {intimacy, targetMemberId} 맵으로 수집
WITH collect({intimacy: innerFriendship.intimacy, targetMemberId: targetMember.id}) AS allEdges, dynamicLimit

-- [4] RETURN: UNWIND 제거, per-node list comprehension
RETURN member.id AS nodeId,
       interestMap[toString(member.id)] AS nodeInterest,
       [e IN topEdges | {friendId: e.targetMemberId, ...}] AS memberEdges
```

### 영향 범위

- `GET /api/v1/networks/me` → `NetworkGraphResult` 반환 (기존 `List<NetworkFriendEdgeResult>` 교체)
- `GET /api/v1/networks/labels/{labelId}` → 동일하게 `NetworkGraphResult` 반환으로 교체
- `NETWORK_PRUNING_SUFFIX` 공유 구조 유지 (두 쿼리 모두 prefix + suffix 패턴)
- `PerfNetworkController` 삭제
- 캐시: `SocialNetworkQueryService.getFriendsNetwork`에 `@Cacheable` 유지

## Cypher DSL → raw string 전환

이번 작업과 함께 `SocialNetworkRepositoryAdapter`의 Cypher DSL(`org.neo4j.cypherdsl`)을 전면 제거하고 raw string 쿼리로 전환했다. DSL은 Java 코드 100줄 이상으로 쿼리 1개를 표현해야 했고, RETURN 구조 변경처럼 Cypher를 직접 다뤄야 하는 시점에 오히려 복잡성이 증가했다. raw string에서 노드/관계 레이블 상수는 `.replace("#{TOKEN}", CONSTANT)` 패턴으로 주입한다.
