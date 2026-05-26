# PLAN: task-29 — 네트워크 조회 쿼리 성능 개선 (interestScore 재탐색 제거)

## 작업 목표

`buildDynamicPruningNetwork()`의 마지막 두 MATCH(각 행마다 `me`의 전체 HAS_FRIENDSHIP 재스캔)를 제거한다.
Step 1에서 `r_me.interestScore`를 map으로 수집하고, CALL 서브쿼리로 Step 3를 격리해 메모리 병목도 해소한다.

- **목표 DB hits**: 5,047,039 → ~190,134 (원본 대비 96% 감소)
- **목표 메모리**: ~12 MB → ~172 KB (원본 대비 99% 감소)

---

## 현황 분석

`buildDynamicPruningNetwork()` 마지막 부분 (L183~192):

```java
.match(relationshipA.relationshipFrom(member, "HAS_FRIENDSHIP"))    // ← 병목: 3,000행 × 재스캔
.match(relationshipB.relationshipFrom(targetNode, "HAS_FRIENDSHIP")) // ← 병목: 3,000행 × 재스캔
.returning(
    ...
    relationshipA.property("interestScore").as("friendA_Interest"),
    relationshipB.property("interestScore").as("friendB_Interest")
)
```

Step 1에서 `r_me`(HAS_FRIENDSHIP 관계 변수)를 수집하지 않아, 마지막 단계에서 동일 경로를 다시 탐색해야 하는 구조.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `SocialNetworkRepositoryAdapter.java` | baseBuilder에 `r_me` 추가, `buildDynamicPruningNetwork` 리팩터링 |
| `SocialNetworkRepositoryAdapterTest.java` | 테스트 데이터 interestScore 값 분화, interestScore 반환값 검증 케이스 추가 |

---

## 구현 방향

### 1. baseBuilder 변경 (두 caller 모두)

`friendshipBetween(me, myFriendship, member)` 대신 명명된 관계 `r_me`를 직접 선언:

```java
Relationship rMe = me.relationshipTo(myFriendship, "HAS_FRIENDSHIP").named("r_me");
// getDefaultIntimacyNetwork
.match(rMe.relationshipFrom(member, "HAS_FRIENDSHIP"))

// getLabelCustomNetwork (기존 label MATCH 뒤에 그대로 추가)
.match(rMe.relationshipFrom(member, "HAS_FRIENDSHIP"))
```

### 2. `buildDynamicPruningNetwork` 시그니처 변경

```java
private Statement buildDynamicPruningNetwork(
    StatementBuilder.OngoingReading baseBuilder,
    Node me, Node member, Node myFriendship, Relationship rMe)
```

### 3. 새 쿼리 흐름

`ApocPatterns.idMapOf()`와 동일한 list comprehension 패턴, `SocialExpansionRepositoryAdapter`의 CALL 서브쿼리 패턴을 그대로 사용:

```
Step A: WITH me, member, myFriendship, r_me ORDER BY intimacy DESC LIMIT $limitSize

Step B: WITH me,
             collect({member, friendship, interestScore: coalesce(r_me.interestScore, 0.0)}) AS friendData

Step C: WITH friendData,
             apoc.coll.union([x IN friendData | x.member], [me]) AS boundary,
             apoc.map.fromPairs([x IN friendData | [toString(x.member.id), x.interestScore]]) AS interestMap

Step D: UNWIND friendData AS item
        WITH boundary, interestMap,
             item.member AS member, item.friendship AS myFriendship

Step E: WITH boundary, interestMap, member,
             toInteger(5 + coalesce(myFriendship.intimacy, 0.0) * 25) AS dynamicLimit

Step F: CALL {                         ← SocialExpansionRepositoryAdapter 동일 패턴
            WITH member, boundary, dynamicLimit
            MATCH (member)-[:HAS_FRIENDSHIP]->(innerFriendship)<-[:HAS_FRIENDSHIP]-(targetMember)
            WHERE targetMember IN boundary AND member <> targetMember
            WITH innerFriendship, targetMember, dynamicLimit
            ORDER BY innerFriendship.intimacy DESC
            RETURN collect({innerFriendship, targetMember})[0..dynamicLimit] AS topEdges
        }

Step G: UNWIND topEdges AS edgeData

Step H: RETURN member.id, edgeData.targetMember.id, edgeData.innerFriendship.intimacy,
              interestMap[toString(member.id)], interestMap[toString(edgeData.targetMember.id)]
```

**DSL 핵심 구현 포인트:**
- list comprehension: `Cypher.listWith(x).in(friendData).returning(...)` — `ApocPatterns.idMapOf()`와 동일
- map subscript: `Cypher.valueAt(interestMap, key)` — 기존 `Cypher.valueAt()` 확장
- CALL 서브쿼리: `Cypher.with(member, boundary, dynamicLimit).match(...).build()` + `.call(innerStatement)` — `SocialExpansionRepositoryAdapter` 동일

### 4. 제거되는 DSL 변수들

`friendshipA`, `friendshipB`, `relationshipA`, `relationshipB`, `targetNode`, `members`, `friendshipList`, `index`, `allEdges`

---

## 예상 사이드 이펙트

- `getIntersectionOneHops`, `getIntersectionByOneHop`, `buildCurrentSkeleton` — 미변경
- `@Cacheable` 및 Redis 캐시 키 — 변경 없음 (반환 타입 동일)
- `NetworkFriendEdgeResult` DTO — 변경 없음

---

## 테스트 전략

기존 5개 케이스(엣지 수, boundary 필터)를 유지하면서:
- 테스트 데이터 일부 `interestScore` 값 분화 (현재 전부 0.0)
- 신규 케이스: `friendA_Interest`가 `interestMap` lookup으로 올바르게 반환되는지 검증
