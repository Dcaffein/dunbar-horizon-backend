# PLAN: task-76 — Neo4j 스키마 상수 일관성 리팩토링

## 작업 목표

1. `@Node` 어노테이션 리터럴 → 도메인 constants로 교체
2. `SchemaInitializer` 두 파일 삭제
3. `SocialConnectionPathRepositoryAdapter` Cypher 쿼리 상수화

---

## 현황 분석

### @Node 하드코딩 현황

| 파일 | 현재 | 교체 상수 |
|------|------|----------|
| `Friendship.java` | `@Node("Friendship")` | `FriendConstants.FRIENDSHIP` |
| `FriendRequest.java` | `@Node("FriendRequest")` | `FriendConstants.FRIEND_REQUEST` |
| `SocialUser.java` | `@Node("SocialUser")` | `SocialUserConstants.SOCIAL_USER` |
| `Label.java` | `@Node("Label")` | `LabelConstants.LABEL` |

### SchemaInitializer

- `SocialUserSchemaInitializer`: `SocialUser.id` unique constraint 생성
- `FriendRequestSchemaInitializer`: `FriendRequest.id` unique constraint 생성 + 레거시 `createdAt` 마이그레이션
- 제약은 DB에서 직접 관리하기로 결정 → 두 파일 삭제

### SocialConnectionPathRepositoryAdapter 하드코딩

```java
MATCH (me:UserReference {id: $myId})-[:HAS_FRIENDSHIP]->(f1:Friendship)<-[r2:HAS_FRIENDSHIP]-(mid:UserReference)
      -[r3:HAS_FRIENDSHIP]->(f2:Friendship)<-[r4:HAS_FRIENDSHIP]-(target:UserReference {id: $targetId})
WHERE r2.isRoutable = true AND r4.isRoutable = true
WITH mid, sqrt(f1.intimacy * f2.intimacy) AS score
...
RETURN mid.id AS userId, mid.nickname AS nickname, score
```

교체 대상:
- `"UserReference"` → `USER_REFERENCE`
- `"Friendship"` → `FRIENDSHIP`
- `"HAS_FRIENDSHIP"` → `HAS_FRIENDSHIP`
- `"isRoutable"` → `PROP_IS_ROUTABLE`
- `"intimacy"` → `PROP_INTIMACY`
- `"id"` → `PROP_ID`
- `"nickname"` → `PROP_NICKNAME`

### GET_LABEL_CUSTOM_NETWORK 버그 수정 (범위 포함)

`SocialNetworkRepositoryAdapter.GET_LABEL_CUSTOM_NETWORK`의 `[:HAS_LABEL]`, `[:HAS_MEMBER]`가 도메인 엔티티(`Label.java`)의 실제 관계 타입(`OWNS_LABEL`, `ATTACHED_TO`)과 달라 항상 빈 결과를 반환 중.

- `[:HAS_LABEL]` → `[:#{OL}]` + `.replace("#{OL}", OWNS_LABEL)`
- `[:HAS_MEMBER]` → `[:#{AT}]` + `.replace("#{AT}", ATTACHED_TO)`
- `LabelConstants.OWNS_LABEL`, `ATTACHED_TO` import 추가

---

## 변경 파일 목록

### 삭제
- `social/adapter/out/persistence/neo4j/schema/FriendRequestSchemaInitializer.java`
- `social/adapter/out/persistence/neo4j/schema/SocialUserSchemaInitializer.java`

### 수정
| 파일 | 변경 내용 |
|------|----------|
| `social/domain/friend/Friendship.java` | `@Node("Friendship")` → `@Node(FriendConstants.FRIENDSHIP)`, import 추가 |
| `social/domain/friend/FriendRequest.java` | `@Node("FriendRequest")` → `@Node(FriendConstants.FRIEND_REQUEST)` (이미 import됨) |
| `social/domain/socialUser/SocialUser.java` | `@Node("SocialUser")` → `@Node(SocialUserConstants.SOCIAL_USER)` (이미 import됨) |
| `social/domain/label/Label.java` | `@Node("Label")` → `@Node(LabelConstants.LABEL)`, import 추가 |
| `social/adapter/out/persistence/neo4j/SocialConnectionPathRepositoryAdapter.java` | 쿼리 `.replace()` 패턴으로 상수화 |

---

## 구현 방향

- `SocialConnectionPathRepositoryAdapter`는 기존 `SocialNetworkRepositoryAdapter`·`SocialExpansionRepositoryAdapter`의 text block + `.replace()` 패턴을 그대로 따른다
- `@Node` 어노테이션의 `static final String` 상수 사용은 Java 컴파일 타임에 상수 폴딩되므로 런타임 영향 없음

---

## 예상 사이드 이펙트

- 없음. 모든 변경은 동일한 문자열 값을 상수로 대체하는 것이므로 런타임 동작 불변

---

## 테스트 전략

관련 단위 테스트 없음 (Neo4j 통합 테스트만 존재). 컴파일 후 `./gradlew test --tests "*.Social*" --tests "*.Friend*" --tests "*.Label*"`로 관련 범위 검증.
