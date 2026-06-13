# Task-74: Neo4j 통합 테스트 격리 및 쿼리 버그 수정

## 배경 및 목적

전체 테스트 실행 시 4개의 Neo4j 통합 테스트가 실패하고 있었다.

```
FriendRequestNeo4jRepositoryTest (3개) — ConstraintValidationFailed: id='1_2' already exists
FriendshipNeo4jRepositoryTest (1개)  — Uncoercible: findFriendshipsByUserId 타입 불일치
```

## 원인 분석

### FriendRequestNeo4jRepositoryTest (ConstraintValidationFailed)

Testcontainers `reuse=true` 환경에서 `@Transactional` 롤백이 제대로 동작하지 않아 이전 테스트 실행 데이터가 Neo4j에 누적된다.

기존 `mergeFriendRequest` 쿼리는 관계 패턴으로 MERGE 후 `ON CREATE SET fr.id = $requestId` 방식이었다:

```cypher
-- 기존 (문제)
MERGE (req)-[:SENT_FRIEND_REQUEST]->(fr:FriendRequest)-[:FRIEND_REQUEST_TO]->(rec)
ON CREATE SET fr.id = $requestId, fr.status = 'PENDING', ...
```

누적된 데이터로 인해 FriendRequest 노드는 존재하지만 관계 패턴이 달라질 경우, MERGE가 새 노드를 생성하려다 unique constraint 위반이 발생한다.

**추가 발견**: `FriendRequestConcurrencyTest`도 함께 점검하였으며, 동시 A→B / B→A 요청이 하나만 성공해야 하는 제약은 unique constraint 위반에 의존한다. MERGE on id로 수정하면 두 방향이 모두 성공하여 동시성 보호가 깨지므로, **CREATE** 를 사용해야 함을 확인했다.

### FriendshipNeo4jRepositoryTest (Uncoercible)

`findFriendshipsByUserId` 쿼리만 다른 패턴을 사용하고 있었다:

```cypher
-- 기존 (문제)
RETURN f, collect(r1), collect(me), collect(r2), collect(friend)

-- 다른 쿼리들 (정상)
RETURN f, collect(all_r), collect(all_u)
```

SDN이 `Friendship`의 `@Relationship(INCOMING) List<FriendRecognition>`을 매핑할 때 두 개의 별도 컬렉션으로 나뉜 패턴을 처리하지 못해 타입 변환 실패가 발생했다.

## 변경 내용

### `FriendRequestNeo4jRepository`

```cypher
-- 수정 후
MATCH (req:UserReference {id: $requesterId}), (rec:UserReference {id: $receiverId})
CREATE (fr:FriendRequest {id: $requestId, status: 'PENDING', createdAt: localdatetime()})
CREATE (req)-[:SENT_FRIEND_REQUEST]->(fr)
CREATE (fr)-[:FRIEND_REQUEST_TO]->(rec)
RETURN fr
```

- MERGE → **CREATE** 교체: unique constraint 위반이 정상적으로 `DataIntegrityViolationException` → `DuplicateFriendRequestException`으로 변환됨
- 동시성 보호(`pairKey` 제약) 유지

### `FriendshipNeo4jRepository`

```cypher
-- 수정 후
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
MATCH (f)<-[all_r:HAS_FRIENDSHIP]-(all_u:UserReference)
RETURN f, collect(all_r), collect(all_u)
```

### 테스트 격리 (`FriendRequestNeo4jRepositoryTest`, `FriendshipNeo4jRepositoryTest`)

`@BeforeEach`에서 `Neo4jClient`를 이용한 전체 노드 삭제 추가:

```java
@Autowired
private Neo4jClient neo4jClient;

@BeforeEach
void setUp() {
    neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    // ... 사용자 노드 재생성
}
```

`@Transactional` 롤백이 Testcontainer `reuse=true` 환경에서 신뢰할 수 없으므로, 각 테스트 시작 전 전체 초기화로 격리를 보장한다.

## 결과

- 481 tests completed, 0 failures
