# PLAN: Task-66 — GET /me 응답을 nodes + edges 조합으로 교체

## Branch
`ai/feat-network-circle-nodes-endpoint` (from `main`)

## 목표

`GET /api/v1/networks/me`의 응답을 `List<NetworkFriendEdgeResult>` → `NetworkGraphResult`로 교체한다.
서비스 레이어에서 nodes 쿼리와 edges 쿼리를 조합(Facade)하여 단일 응답으로 반환하고,
`@Cacheable`을 service 레이어로 올려 `NetworkGraphResult` 전체를 캐시한다.

---

## 현황 분석

### 문제
edges만 반환하는 현재 구조에서는 다른 친구와 연결이 없는 고립 노드가 프론트엔드에 렌더링되지 않는다. circleSize 판단 권한은 서버에 있어야 하므로 클라이언트가 친구 목록으로 노드를 직접 계산하는 것은 비즈니스 로직 유출이다.

### 설계 결정: Facade at Service Layer
두 쿼리를 하나의 Neo4j 쿼리로 합치지 않는다(기존 `buildDynamicPruningNetwork` 복잡도 유지).
대신 서비스 레이어에서 두 쿼리를 조합하고, 조합된 결과를 통째로 캐시한다.

nodes와 edges는 항상 함께 필요한 한 단위이므로 캐시도 한 단위로 관리한다.

---

## @Cacheable을 service 레이어로 올릴 때의 주의사항

### 문제 1 — AOP 프록시 실행 순서 (`@Neo4jTransactional` 충돌)

Spring은 `@Transactional`과 `@Cacheable`을 각각 AOP 인터셉터로 적용한다.
현재 `SocialNetworkQueryService`에 `@Neo4jTransactional(readOnly = true)`가 클래스 레벨에 선언되어 있다.

두 어노테이션이 같은 클래스에 있을 때 기본 실행 순서:

```
호출자
  → TransactionInterceptor (Neo4j) ← 바깥 레이어
      → CacheInterceptor             ← 안쪽 레이어
          → 실제 메서드
```

`TransactionInterceptor`가 바깥을 감싸므로 **캐시 히트 시에도** 아래 사이클이 실행된다:

```
1. Neo4j 커넥션 풀에서 세션 획득
2. BEGIN 패킷 전송 → Neo4j 왕복 대기 (~15ms)
3. CacheInterceptor → Redis 히트, 즉시 반환
4. COMMIT 패킷 전송 → Neo4j 왕복 대기 (~15ms)
5. 세션 반납
```

이는 load test Phase 4의 병목 원인이었다 (캐시 히트율 ~100%인데 평균 230ms).

**해결**: `SocialNetworkQueryService` 클래스 레벨의 `@Neo4jTransactional(readOnly = true)` 제거.
서비스가 호출하는 모든 repo 메서드는 `Neo4jClient`를 직접 사용하며,
`Neo4jClient`는 트랜잭션 어노테이션 없이 auto-commit 모드로 읽기 쿼리를 실행한다.

### 문제 2 — `cacheManager` ObjectMapper의 `activateDefaultTyping` 버그

`RedisConfig.cacheManager`의 ObjectMapper에 `activateDefaultTyping(NON_FINAL)`이 설정되어 있다.
이 설정은 직렬화 시 모든 non-final 타입에 `@class` 메타데이터를 삽입한다.

`NetworkGraphResult` 역직렬화 시 타입 포맷 불일치로 예외가 발생하고,
`CacheErrorHandler`가 예외를 조용히 삼키며 Neo4j로 fall-through한다.
기능상 정상 동작하지만 캐시가 완전히 무효 상태가 된다. (load test Phase 4 초기 버그와 동일)

**해결**: `cacheManager` ObjectMapper에서 `activateDefaultTyping` 제거.
`NetworkGraphResult`는 구체 타입이므로 폴리모픽 타입 정보 불필요.
(`redisTemplate`용 ObjectMapper는 eviction 등 별도 용도이므로 그대로 유지)

### 두 수정이 이번 task 범위인 이유
`@Cacheable`을 service 레이어로 올리면 위 두 문제가 반드시 함께 터진다.
별도 task로 분리하면 캐시가 동작하지 않거나 Phase 4 수준에 머문다.
load test에서 이미 검증된 수정사항이므로 범위에 포함한다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `global/config/RedisConfig.java` | `cacheManager` ObjectMapper에서 `activateDefaultTyping` 제거 |
| `social/application/dto/result/NetworkGraphResult.java` | 신규: `record NetworkGraphResult(List<Long> nodeIds, List<NetworkFriendEdgeResult> edges)` |
| `social/application/port/out/SocialNetworkRepository.java` | `getCircleNodeIds(Long userId, DunbarCircle circleSize)` 추가 |
| `social/application/port/in/SocialNetworkQueryUseCase.java` | `getFriendsNetwork` 반환 타입 → `NetworkGraphResult` |
| `social/application/service/SocialNetworkQueryService.java` | 클래스 레벨 `@Neo4jTransactional` 제거. `getFriendsNetwork`에 `@Cacheable` 추가 + 두 쿼리 조합 |
| `social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java` | `getDefaultIntimacyNetwork`의 `@Cacheable` 제거. `buildDynamicPruningNetwork` DSL → raw Cypher string 전환. `getCircleNodeIds` 구현 추가. `@Transactional(readOnly = true)` 제거 |
| `social/adapter/in/web/SocialQueryController.java` | `GET /me` 반환 타입 → `ResponseEntity<NetworkGraphResult>` |

---

## 구현 방향

### 신규 DTO

```java
public record NetworkGraphResult(
        List<Long> nodeIds,
        List<NetworkFriendEdgeResult> edges
) {}
```

### Service

```java
@Cacheable(cacheNames = "dunbar:network:default", key = "#userId + ':' + #circleSize.name()")
@Override
public NetworkGraphResult getFriendsNetwork(Long userId, DunbarCircle circleSize) {
    List<NetworkFriendEdgeResult> edges = socialNetworkRepository.getDefaultIntimacyNetwork(userId, circleSize);
    List<Long> nodeIds = socialNetworkRepository.getCircleNodeIds(userId, circleSize);
    return new NetworkGraphResult(nodeIds, edges);
}
```

클래스 레벨 `@Neo4jTransactional(readOnly = true)` 제거.
나머지 메서드(`getLabelNetwork`, `getNewNodeEdges`, `getNetworkContactsOfTwoHop`)는 변경 없음.

### Nodes Cypher (raw string 상수)

```cypher
MATCH (me:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)<-[:HAS_FRIENDSHIP]-(friend:UserReference)
WITH friend, f.intimacy AS intimacy
ORDER BY intimacy DESC
LIMIT $circleSize
RETURN friend.id AS friendId
```

### Cache key 형식 (캐시된 실제 Redis 키)

`dunbar:network:default:{userId}:{circleSize}` → 기존 eviction 로직 그대로 적용 가능

---

## 예상 사이드 이펙트

- `GET /me` 응답 스키마 변경 → 프론트엔드 수정 필요
- `getLabelNetwork`는 변경하지 않음 (라벨 멤버 목록이 곧 노드 정의)
- `SocialNetworkQueryServiceTest`: `getFriendsNetwork` 관련 테스트 수정 필요

---

## 테스트 전략

**Repository 통합 테스트 (`SocialNetworkRepositoryAdapterTest` 기존 그래프 재사용):**
- `getCircleNodeIds(1L, DUNBAR)` → 친구 6명 전원 반환
- `getCircleNodeIds(1L, SUPPORT)` → intimacy 상위 5명 반환 (F(60, 0.1) 제외)
- `getCircleNodeIds(999L, DUNBAR)` → 빈 리스트

**Service 단위 테스트 (`SocialNetworkQueryServiceTest`):**
- `getFriendsNetwork` 호출 시 `getDefaultIntimacyNetwork`와 `getCircleNodeIds` 둘 다 호출되는지
- 반환값이 `NetworkGraphResult(nodeIds, edges)` 로 올바르게 조합되는지
