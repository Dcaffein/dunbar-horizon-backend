# PLAN: Task-29 — 네트워크 조회 쿼리 성능 개선 (개선 2: CALL 서브쿼리)

## 작업 목표

`buildDynamicPruningNetwork`를 개선 2 구조로 전면 재작성한다.

1. Step 1: `r_me.interestScore`를 map(`friendData`)으로 수집 + `interestMap` 구성
2. Step 2: `range → index` 패턴을 `UNWIND friendData AS item`으로 교체
3. Step 3: 마지막 두 MATCH 제거 + `CALL (member, boundary, dynamicLimit) { ... }` 서브쿼리 도입
4. RETURN: `relationshipA/B.interestScore` → `interestMap[toString(member.id)]` lookup으로 교체

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `social/adapter/out/SocialNetworkRepositoryAdapter.java` | `buildDynamicPruningNetwork` 전면 재작성, 두 caller에 `r_me` 추가 |

---

## 구현 방향

### 1. Caller 변경 (getDefaultIntimacyNetwork / getLabelCustomNetwork)

`r_me` 관계 변수를 정의하고 baseBuilder MATCH 패턴에 포함시킨다.

```java
// 현재
StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
        .match(friendshipBetween(me, myFriendship, member));
Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship);

// 개선
Relationship r_me = me.relationshipTo(myFriendship, HAS_FRIENDSHIP).named("r_me");
StatementBuilder.OngoingReading baseBuilder = matchUserWithId(userId, "me")
        .match(r_me.relationshipFrom(member, HAS_FRIENDSHIP));
Statement statement = buildDynamicPruningNetwork(baseBuilder, me, member, myFriendship, r_me);
```

### 2. buildDynamicPruningNetwork 시그니처 변경

```java
// 현재
private Statement buildDynamicPruningNetwork(
        StatementBuilder.OngoingReading baseBuilder,
        Node me, Node member, Node myFriendship)

// 개선
private Statement buildDynamicPruningNetwork(
        StatementBuilder.OngoingReading baseBuilder,
        Node me, Node member, Node myFriendship, Relationship r_me)
```

### 3. Step 1 — friendData + interestMap 수집

```java
SymbolicName friendData  = Cypher.name("friendData");
SymbolicName interestMap = Cypher.name("interestMap");
SymbolicName x           = Cypher.name("x");

// collect({member, friendship, interestScore})
Expression friendDataMap = Cypher.mapOf(
        "member",        member.getRequiredSymbolicName(),
        "friendship",    myFriendship.getRequiredSymbolicName(),
        "interestScore", Cypher.coalesce(r_me.property("interestScore"), Cypher.literalOf(0.0))
);

// [x IN friendData | x.member]
Expression memberList = Cypher.listWith(x).in(friendData)
        .returning(Cypher.property(x, "member"));

// [x IN friendData | [toString(x.member.id), x.interestScore]]
Expression interestPairs = Cypher.listWith(x).in(friendData)
        .returning(Cypher.listOf(
                Cypher.call("toString")
                        .withArgs(Cypher.property(Cypher.property(x, "member"), PROP_ID))
                        .asFunction(),
                Cypher.property(x, "interestScore")
        ));

StatementBuilder.OngoingReadingAndWith withBoundary = baseBuilder
        .with(me, member, myFriendship, r_me)
        .orderBy(myFriendship.property(PROP_INTIMACY).descending())
        .limit(Cypher.parameter("limitSize"))
        .with(me, Cypher.collect(friendDataMap).as("friendData"))
        .with(
                friendData,
                Cypher.call("apoc.coll.union")
                        .withArgs(memberList, Cypher.listOf(me.getRequiredSymbolicName()))
                        .asFunction().as("boundary"),
                Cypher.call("apoc.map.fromPairs")
                        .withArgs(interestPairs)
                        .asFunction().as("interestMap")
        );
```

### 4. Step 2 — UNWIND friendData (range·index 패턴 제거)

```java
SymbolicName item        = Cypher.name("item");
SymbolicName boundary    = Cypher.name("boundary");
SymbolicName dynamicLimit = Cypher.name("dynamicLimit");

Expression myIntimacy = Cypher.coalesce(
        Cypher.property(Cypher.property(item, "friendship"), PROP_INTIMACY),
        Cypher.literalOf(0.0)
);
Expression limitCalc = Cypher.call("toInteger")
        .withArgs(Cypher.literalOf(5).add(myIntimacy.multiply(Cypher.literalOf(25))))
        .asFunction();

StatementBuilder.OngoingReadingAndWith withLimit = withBoundary
        .unwind(friendData).as("item")
        .with(
                boundary,
                interestMap,
                Cypher.property(item, "member").as(member.getRequiredSymbolicName().getValue()),
                Cypher.property(item, "friendship").as(myFriendship.getRequiredSymbolicName().getValue())
        )
        .with(boundary, interestMap, member, limitCalc.as("dynamicLimit"));
```

### 5. Step 3 — CALL 서브쿼리

```java
Node innerFriendship = friendship().named("innerFriendship");
Node targetMember    = user().named("targetMember");
SymbolicName topEdges = Cypher.name("topEdges");
SymbolicName edgeData = Cypher.name("edgeData");

Expression edgeMap = Cypher.mapOf(
        "innerFriendship", innerFriendship.getRequiredSymbolicName(),
        "targetMember",    targetMember.getRequiredSymbolicName()
);

// 서브쿼리 내부 Statement
Statement subStatement = Cypher
        .match(friendshipBetween(member, innerFriendship, targetMember))
        .where(targetMember.getRequiredSymbolicName().in(boundary)
                .and(Cypher.not(Cypher.elementId(member).isEqualTo(Cypher.elementId(targetMember)))))
        .with(innerFriendship, targetMember, dynamicLimit)
        .orderBy(innerFriendship.property(PROP_INTIMACY).descending())
        .returning(
                Cypher.subList(Cypher.collect(edgeMap), Cypher.literalOf(0), dynamicLimit)
                        .as("topEdges")
        )
        .build();
```

### 6. RETURN — interestMap lookup

```java
return withLimit
        .call(subStatement, member, boundary, dynamicLimit)  // CALL 서브쿼리
        .unwind(topEdges).as("edgeData")
        .returning(
                member.property(PROP_ID).as("friendA_Id"),
                Cypher.property(Cypher.property(edgeData, "targetMember"), PROP_ID).as("friendB_Id"),
                Cypher.property(Cypher.property(edgeData, "innerFriendship"), PROP_INTIMACY).as("intimacy"),
                Cypher.valueAt(interestMap,
                        Cypher.call("toString").withArgs(member.property(PROP_ID)).asFunction())
                        .as("friendA_Interest"),
                Cypher.valueAt(interestMap,
                        Cypher.call("toString")
                                .withArgs(Cypher.property(Cypher.property(edgeData, "targetMember"), PROP_ID))
                                .asFunction())
                        .as("friendB_Interest")
        )
        .build();
```

---

## 제거되는 변수 (현재 코드에서 삭제)

`friendshipA`, `friendshipB`, `targetNode`, `relationshipA`, `relationshipB`,
`members`, `friendshipList`, `index`, `allEdges`

---

## 브랜치

`ai/feat-optimize-network-query`
