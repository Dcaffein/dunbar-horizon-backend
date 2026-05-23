# Task: 네트워크 조회 쿼리 성능 개선 — interestScore 재탐색 제거

## Background

`GET /api/v1/networks/me` 는 `SocialNetworkRepositoryAdapter.getDefaultIntimacyNetwork()`가 실행하는
Neo4j Cypher 쿼리로 구동된다.

Neo4j Desktop에서 실사용자(userId=2013, 친구 198명, 각 친구도 198명 보유)를 대상으로 PROFILE 실행 결과:

```
Total database accesses : 5,047,039
Total allocated memory  : 12,069,088 bytes (~12 MB)
Total time              : 1,663 ms
```

Redis 캐시가 있어 캐시 히트 시에는 문제없으나, 캐시 무효화 직후(친구 추가·삭제 직후 양쪽 유저의 첫 요청)마다
이 비용이 그대로 발생한다.

### 병목 구간

쿼리 마지막 단계에서 `topEdges`를 UNWIND한 뒤 **각 행마다** 두 번의 MATCH가 추가 실행된다.

```cypher
-- topEdges 행마다 반복 (최대 150명 × dynamicLimit ≈ 3,000~4,500회)
MATCH (me)-[relationshipA:HAS_FRIENDSHIP]->(friendshipA:Friendship)<-[:HAS_FRIENDSHIP]-(member)
MATCH (me)-[relationshipB:HAS_FRIENDSHIP]->(friendshipB:Friendship)<-[:HAS_FRIENDSHIP]-(targetNode)
```

각 MATCH는 `me`의 HAS_FRIENDSHIP 관계(198개)를 전수 스캔하므로, 대략:

```
3,000행 × 2 MATCH × ~198 hits ≈ 1,188,000 db hits  (마지막 두 MATCH 기여분)
+ Step 3 경계 스캔 150명 × 198 + 정렬·collect 등 나머지
→ 5,047,039
```

### 근본 원인

Step 1에서 `(me)-[r_me:HAS_FRIENDSHIP]->(myFriendship)` 을 탐색할 때
`r_me.interestScore`(HAS_FRIENDSHIP 관계 프로퍼티)를 함께 수집하지 않고,
`Friendship` 노드(`myFriendship`)만 리스트에 담았다.

이후 `collect → unwind` 과정에서 관계 정보가 소실되어,
마지막 단계에서 동일 경로를 다시 탐색해야 하는 구조가 됐다.

## Objective

- 마지막 두 MATCH를 제거하여 db hits를 대폭 감소시킨다.
- Step 1 탐색 시점에 `r_me.interestScore`를 map으로 함께 수집하고,
  이후 단계에서 재탐색 없이 직접 참조한다.
- `friendA_Interest`(me → friendA)와 `friendB_Interest`(me → friendB) 모두 map lookup으로 해결한다.

## Domain Change

[ ] 없음  [x] 있음
- `SocialNetworkRepositoryAdapter.buildDynamicPruningNetwork()` 쿼리 로직 변경
- `getDefaultIntimacyNetwork()` / `getLabelCustomNetwork()` baseBuilder 변경
  (관계 변수 `r_me` 추가 및 interestScore 수집)

---

## 개선 단계

### 현재 상태 (개선 전)

```
DB hits  : 5,047,039
Memory   : ~12 MB
Time     : 1,663 ms
```

Step 3에서 topEdges UNWIND 후 각 행마다 두 번의 MATCH를 재실행하여 `interestScore`를 조회하는 구조.
Step 3 전체가 하나의 flat WITH 체인으로 이루어져 있어 22,500행 전체를 한 번에 정렬 및 collect함.

---

### 개선 1 — interestScore map 수집 + 마지막 두 MATCH 제거

Step 1에서 `r_me.interestScore`를 map에 함께 수집하고,
`apoc.map.fromPairs`로 `interestMap`을 구성하여 RETURN 시 직접 lookup한다.
기존 `collect(member) → range → unwind index` 패턴을 단일 map 리스트 `friendData`로 교체한다.

```cypher
-- Step 1 변경: interestScore를 map에 수집
MATCH (me)-[r_me:HAS_FRIENDSHIP]->(myFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(member)
...
collect({
    member:        member,
    friendship:    myFriendship,
    interestScore: coalesce(r_me.interestScore, 0.0)
}) AS friendData

WITH friendData,
     apoc.coll.union([x IN friendData | x.member], [me]) AS boundary,
     apoc.map.fromPairs([x IN friendData | [toString(x.member.id), x.interestScore]]) AS interestMap

UNWIND friendData AS item
WITH boundary, interestMap,
     item.member     AS member,
     item.friendship AS myFriendship

-- 마지막 두 MATCH 제거 후 RETURN
RETURN
    member.id                                       AS friendA_Id,
    edgeData.targetMember.id                        AS friendB_Id,
    edgeData.innerFriendship.intimacy               AS intimacy,
    interestMap[toString(member.id)]                AS friendA_Interest,
    interestMap[toString(edgeData.targetMember.id)] AS friendB_Interest
```

**결과 (Cypher 검증 완료, 최악 케이스 기준):**

```
DB hits  : 212,634   (96% 감소)
Memory   : ~14.7 MB  (악화 — 22,500행 정렬이 잔여 병목)
Time     : 756 ms    (55% 감소)
```

잔여 병목: Step 3 인메모리 정렬 22,500행 (Sort 4.4MB + EagerAggregation 10.2MB).
그래프 위상 자체 탐색으로 알고리즘 변경 없이는 추가 감소 어려움.

---

### 개선 2 — CALL 서브쿼리로 Step 3 격리 (메모리 병목 해소)

Step 3의 MATCH·ORDER BY·collect를 `CALL (member, boundary, dynamicLimit) { ... }` 서브쿼리로 격리한다.
서브쿼리는 member 1명분(~200행)씩 독립 실행되어 정렬·수집 후 즉시 해제되므로,
22,500행 전체를 동시에 메모리에 올리는 문제가 해소된다.

```cypher
CALL (member, boundary, dynamicLimit) {
    MATCH (member)-[:HAS_FRIENDSHIP]->(innerFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(targetMember)
    WHERE targetMember IN boundary AND elementId(member) <> elementId(targetMember)
    WITH innerFriendship, targetMember, dynamicLimit
    ORDER BY innerFriendship.intimacy DESC
    RETURN collect({
        innerFriendship: innerFriendship,
        targetMember:    targetMember
    })[0..dynamicLimit] AS topEdges
}

UNWIND topEdges AS edgeData

RETURN
    member.id                                       AS friendA_Id,
    edgeData.targetMember.id                        AS friendB_Id,
    edgeData.innerFriendship.intimacy               AS intimacy,
    interestMap[toString(member.id)]                AS friendA_Interest,
    interestMap[toString(edgeData.targetMember.id)] AS friendB_Interest
```

**결과 (Cypher 검증 완료, 최악 케이스 기준):**

```
DB hits  : 190,134   (개선 1 대비 10% 추가 감소 / 원본 대비 96% 감소)
Memory   : ~172 KB   (개선 1 대비 99% 감소 / 원본 대비 99% 감소)
Time     : 미측정
```

메모리 분석:
- Sort (서브쿼리 1회 기준): 121,952 bytes
- EagerAggregation (서브쿼리 1회 기준): 53,984 bytes
- 피크가 150회 반복이 아닌 1회분으로 고정됨

**구현 참고:** `neo4j-cypher-dsl 2024.2.0`의 `ExposesSubqueryCall.call(Statement, IdentifiableElement...)` API로 DSL 표현 가능.

---

## 구현 대상

개선 2를 최종 구현 목표로 한다 (개선 1을 건너뛰고 바로 개선 2 적용).

---

## Cypher Before / After

### Before

```cypher
MATCH (me:UserReference) WHERE me.id = $userId
MATCH (me)-[:HAS_FRIENDSHIP]->(myFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(member:UserReference)
WITH me, member, myFriendship
ORDER BY myFriendship.intimacy DESC LIMIT $limitSize

WITH me,
     collect(member)       AS members,
     collect(myFriendship) AS friendshipList
WITH me,
     apoc.coll.union(members, [me]) AS boundary,
     members,
     friendshipList

UNWIND range(0, size(members)-1) AS index
WITH me, boundary,
     members[index]        AS member,
     friendshipList[index] AS myFriendship
WITH me, boundary, member,
     toInteger(5 + coalesce(myFriendship.intimacy, 0.0) * 25) AS dynamicLimit

MATCH (member)-[:HAS_FRIENDSHIP]->(innerFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(targetMember)
WHERE targetMember IN boundary
WITH me, member, dynamicLimit, innerFriendship, targetMember
ORDER BY innerFriendship.intimacy DESC
WITH me, member, dynamicLimit,
     collect({innerFriendship: innerFriendship, targetMember: targetMember}) AS allEdges
WITH me, member,
     allEdges[0..dynamicLimit] AS topEdges
UNWIND topEdges AS edgeData
WITH me, member, edgeData, edgeData.targetMember AS targetNode

-- ⚠️ 병목: 각 행마다 me의 HAS_FRIENDSHIP 전수 스캔 (~3,000행 × 2회)
MATCH (me)-[relationshipA:HAS_FRIENDSHIP]->(friendshipA:Friendship)<-[:HAS_FRIENDSHIP]-(member)
MATCH (me)-[relationshipB:HAS_FRIENDSHIP]->(friendshipB:Friendship)<-[:HAS_FRIENDSHIP]-(targetNode)

RETURN
    member.id                         AS friendA_Id,
    targetNode.id                     AS friendB_Id,
    edgeData.innerFriendship.intimacy AS intimacy,
    relationshipA.interestScore       AS friendA_Interest,
    relationshipB.interestScore       AS friendB_Interest
```

### After

```cypher
MATCH (me:UserReference) WHERE me.id = $userId
MATCH (me)-[r_me:HAS_FRIENDSHIP]->(myFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(member:UserReference)
WITH me, member, myFriendship, r_me
ORDER BY myFriendship.intimacy DESC LIMIT $limitSize

WITH me,
     collect({
         member:        member,
         friendship:    myFriendship,
         interestScore: coalesce(r_me.interestScore, 0.0)
     }) AS friendData

WITH friendData,
     apoc.coll.union([x IN friendData | x.member], [me])                         AS boundary,
     apoc.map.fromPairs([x IN friendData | [toString(x.member.id), x.interestScore]]) AS interestMap

UNWIND friendData AS item
WITH boundary, interestMap,
     item.member     AS member,
     item.friendship AS myFriendship

WITH boundary, interestMap, member,
     toInteger(5 + coalesce(myFriendship.intimacy, 0.0) * 25) AS dynamicLimit

CALL (member, boundary, dynamicLimit) {
    MATCH (member)-[:HAS_FRIENDSHIP]->(innerFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(targetMember)
    WHERE targetMember IN boundary AND elementId(member) <> elementId(targetMember)
    WITH innerFriendship, targetMember, dynamicLimit
    ORDER BY innerFriendship.intimacy DESC
    RETURN collect({
        innerFriendship: innerFriendship,
        targetMember:    targetMember
    })[0..dynamicLimit] AS topEdges
}

UNWIND topEdges AS edgeData

RETURN
    member.id                                       AS friendA_Id,
    edgeData.targetMember.id                        AS friendB_Id,
    edgeData.innerFriendship.intimacy               AS intimacy,
    interestMap[toString(member.id)]                AS friendA_Interest,
    interestMap[toString(edgeData.targetMember.id)] AS friendB_Interest
```

## Result

> 구현 완료 후 작성

- 브랜치:
- 커밋:
