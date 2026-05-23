# Network Query 성능 분석 및 개선 제안

> 분석 대상: `SocialNetworkRepositoryAdapter.getDefaultIntimacyNetwork` / `buildDynamicPruningNetwork`
> 파일 위치: `social/adapter/out/SocialNetworkRepositoryAdapter.java`

---

## 현재 쿼리 구조 요약

`GET /api/v1/networks/me?circleSize=X` 호출 시 실행되는 Cypher 쿼리는 세 단계로 구성된다.

### Step 1 — Top-N 친구 추출 + boundary 구성

```cypher
MATCH (me:UserReference {id: $userId})
MATCH (me)-[:HAS_FRIENDSHIP]->(myFriendship:Friendship)<-[:HAS_FRIENDSHIP]-(member:UserReference)
WITH me, member, myFriendship
ORDER BY myFriendship.intimacy DESC
LIMIT $limitSize                          -- circleSize에 따라 5/15/50/150
WITH me,
     collect(member)       AS members,
     collect(myFriendship) AS friendshipList
WITH me,
     apoc.coll.union(members, [me]) AS boundary,  -- 탐색 경계 = Top-N + 나 자신
     members,
     friendshipList
```

### Step 2 — 친구별 dynamicLimit 계산

```cypher
UNWIND range(0, size(members)-1) AS index
WITH me, boundary,
     members[index]       AS member,
     friendshipList[index] AS myFriendship
WITH me, boundary, member,
     toInteger(5 + coalesce(myFriendship.intimacy, 0.0) * 25) AS dynamicLimit
```

| intimacy | dynamicLimit |
|----------|-------------|
| 0.0      | 5           |
| 0.5      | 17          |
| 1.0      | 30          |

### Step 3 — 엣지 Pruning + interestScore 조회

```cypher
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

-- ⚠️ 문제 구간: 각 행마다 두 번의 MATCH 추가 실행
MATCH (me)-[relationshipA:HAS_FRIENDSHIP]->(friendshipA)<-[:HAS_FRIENDSHIP]-(member)
MATCH (me)-[relationshipB:HAS_FRIENDSHIP]->(friendshipB)<-[:HAS_FRIENDSHIP]-(targetNode)

RETURN
    member.id                               AS friendA_Id,
    targetNode.id                           AS friendB_Id,
    edgeData.innerFriendship.intimacy       AS intimacy,
    relationshipA.interestScore             AS friendA_Interest,
    relationshipB.interestScore             AS friendB_Interest
```

---

## 성능 문제 분석

### 문제 1 — 마지막 두 MATCH 반복 (가장 심각)

`relationshipA`와 `relationshipB`는 `interestScore`를 꺼내기 위해 존재한다.

- `relationshipA` (`me → member`): Step 1에서 이미 탐색한 경로와 동일하지만, `collect → unwind` 과정에서 relationship 정보가 소실되어 재탐색 필요
- `relationshipB` (`me → targetNode`): targetNode도 boundary 내 노드이므로 이미 Step 1에서 탐색된 경로

**최악의 경우 반복 횟수:**
```
DUNBAR(150명) × dynamicLimit 최대(30) = 4,500번 MATCH 실행
```

캐시 미스 직후(친구 추가/삭제 직후 첫 요청)에 이 비용이 그대로 발생한다.

### 문제 2 — collect → unwind index 패턴

```cypher
UNWIND range(0, size(members)-1) AS index
WITH members[index] AS member, friendshipList[index] AS myFriendship
```

Cypher에 두 리스트를 동시에 zip하는 기본 문법이 없어 index 기반으로 우회한 것이다. 이 패턴 자체가 Step 1의 relationship 정보를 리스트로 담지 못하게 만드는 근본 원인이다.

### 문제 3 — member마다 ORDER BY 반복

Step 3에서 각 member(최대 150명)에 대해 `innerFriendship.intimacy` 기준 정렬이 반복된다. Neo4j query planner가 부분 최적화하더라도 명시적 정렬이 member 수만큼 발생하는 구조다.

### 문제 4 — 현재 캐싱이 성능을 보완하는 구조

`@Cacheable(cacheNames = "dunbar:network:default", key = "#userId + ':' + #circleSize.name()")`

Redis 캐시 히트 시 Neo4j 쿼리 자체를 건너뛰므로 실 사용에서는 대부분 문제없다. 그러나 다음 이벤트 발생 시 캐시가 무효화되고 비용이 집중된다:

| 이벤트 | 무효화 범위 |
|--------|------------|
| 친구 요청 수락 | 요청자 + 수신자 default 네트워크 전체 (4개 circleSize) |
| 친구 삭제 | 양쪽 default + 모든 label 네트워크 |

---

## 개선 제안

### 제안 1 — Step 1에서 interestScore를 함께 수집 (핵심)

마지막 두 MATCH의 목적은 `me → member`, `me → targetNode`의 `interestScore`를 얻는 것이다. Step 1 탐색 시점에 `HAS_FRIENDSHIP` 관계(`r_me`)를 함께 수집하면 재탐색을 완전히 제거할 수 있다.

```cypher
-- 현재
MATCH (me)-[:HAS_FRIENDSHIP]->(myFriendship)<-[:HAS_FRIENDSHIP]-(member)
...
collect(member) AS members, collect(myFriendship) AS friendshipList

-- 개선: map으로 필요한 값을 미리 담기
MATCH (me)-[r_me:HAS_FRIENDSHIP]->(myFriendship)<-[:HAS_FRIENDSHIP]-(member)
...
collect({
    member:        member,
    friendship:    myFriendship,
    interestScore: r_me.interestScore   -- 여기서 미리 수집
}) AS myFriendData
```

이렇게 하면:
- `friendA_Interest` → map에서 직접 참조, MATCH 불필요
- `friendB_Interest` → targetNode도 boundary 내 노드이므로 `myFriendData` map에서 lookup 가능

**예상 효과**: 최악 케이스 4,500번 MATCH → 0번

### 제안 2 — APOC zip으로 index 패턴 대체

```cypher
-- 현재 (index 기반 우회)
UNWIND range(0, size(members)-1) AS index
WITH members[index] AS member, friendshipList[index] AS myFriendship

-- 개선 (APOC zip 사용)
UNWIND apoc.coll.zip(myFriendData, myFriendData) AS pair
-- 또는 제안 1처럼 처음부터 map으로 묶어서 단일 리스트로 관리
UNWIND myFriendData AS item
WITH item.member AS member, item.friendship AS myFriendship, item.interestScore AS myInterestScore
```

제안 1을 적용하면 이 패턴도 자연스럽게 해결된다.

### 제안 3 — friendB_Interest map lookup

`targetNode`는 boundary 내 노드이므로, `myFriendData`를 map으로 구성해두면 id로 직접 조회할 수 있다.

```cypher
-- boundary 노드의 interestScore를 미리 map으로 준비
WITH apoc.map.fromPairs([x IN myFriendData | [toString(x.member.id), x.interestScore]]) AS interestMap
...
-- friendB_Interest 조회
interestMap[toString(targetNode.id)] AS friendB_Interest
```

### 제안 4 — 개선 우선순위

| 우선순위 | 제안 | 난이도 | 효과 |
|---------|------|--------|------|
| 1 | Step 1에서 interestScore 수집 + 마지막 두 MATCH 제거 | 중 | 최악 케이스 N×M 탐색 제거 |
| 2 | APOC zip 또는 단일 map 리스트로 index 패턴 제거 | 하 | 코드 가독성 향상 |
| 3 | friendB_Interest를 interestMap lookup으로 처리 | 중 | 제안 1과 묶어서 처리 |

---

## 현재 운용 판단

캐시 히트율이 높은 상황에서는 실용적으로 동작하는 쿼리다. 단, 다음 조건이 겹칠 경우 문제가 표면화될 수 있다:

- 친구 수가 많은 유저(DUNBAR 근접)
- 친구 추가/삭제가 빈번한 시점 (캐시 무효화 반복)
- 다수 유저가 동시에 첫 요청을 보내는 경우 (캐시 미스 폭발)

개선 작업은 캐시 무효화 빈도가 높아지는 시점 전에 적용하는 것이 적절하다.
