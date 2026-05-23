# Task: 네트워크 mutual 조회 labelName 파라미터를 labelId로 변경

## Background

`/api/v1/networks/mutual/one-hop`과 `/api/v1/networks/mutual/two-hop` 엔드포인트는
현재 뷰의 라벨 컨텍스트를 `labelName`(문자열)으로 전달받는다.

그러나 라벨 관련 나머지 모든 엔드포인트는 `labelId`를 식별자로 사용한다.

```
GET /api/v1/labels/{labelId}              → labelId 사용 ✓
PATCH /api/v1/labels/{labelId}            → labelId 사용 ✓
GET /api/v1/networks/labels/{labelId}     → labelId 사용 ✓
GET /api/v1/networks/mutual/one-hop?labelName=  → labelName 사용 ✗
GET /api/v1/networks/mutual/two-hop?labelName=  → labelName 사용 ✗
```

`labelName`은 사용자가 변경할 수 있는 값이므로 식별자로 사용하면 안 되고,
프론트엔드 orval 스펙에도 다른 라벨 API와 타입이 불일치하는 문제가 있다.

## Objective

- `one-hop` / `two-hop` 엔드포인트의 `labelName` 파라미터를 `labelId`로 변경한다.
- Controller → UseCase → Repository 포트 → Repository 어댑터까지 일괄 변경한다.
- Neo4j `buildCurrentSkeleton` 쿼리의 `label.name` 필터를 `label.id` 필터로 교체한다.

## Domain Change

[ ] 없음  [x] 있음
- `SocialQueryController` — `@RequestParam String labelName` → `@RequestParam String labelId`
- `SocialNetworkQueryUseCase` — 메서드 시그니처 파라미터명 변경
- `SocialNetworkQueryService` — 파라미터명 변경
- `SocialNetworkRepository` (포트) — 메서드 시그니처 파라미터명 변경
- `SocialNetworkRepositoryAdapter.buildCurrentSkeleton()` — Neo4j 쿼리 변경

## Decision

### Controller

```java
// 현재
@RequestParam(required = false) String labelName

// 개선
@RequestParam(required = false) String labelId
```

### buildCurrentSkeleton Neo4j 쿼리

```java
// 현재: label name으로 필터
builder = builder
    .match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(myFriend, "HAS_MEMBER"))
    .where(label.property("name").isEqualTo(Cypher.parameter("labelName")));
...
.bind(labelName == null ? "" : labelName).to("labelName")

// 개선: label id로 필터
builder = builder
    .match(me.relationshipTo(label, "HAS_LABEL").relationshipTo(myFriend, "HAS_MEMBER"))
    .where(label.property("id").isEqualTo(Cypher.parameter("labelId")));
...
.bind(labelId == null ? "" : labelId).to("labelId")
```

### null 처리 조건 변경

현재 `buildCurrentSkeleton`은 `labelName != null && !labelName.isBlank()` 조건으로 분기한다.
`labelId`도 동일한 null/blank 조건을 유지한다.

## Result

- 브랜치: `ai/fix-label-context-param-to-id`
- 커밋: `e823059`
- 변경 파일 5개: `SocialQueryController`, `SocialNetworkQueryUseCase`, `SocialNetworkQueryService`, `SocialNetworkRepository`, `SocialNetworkRepositoryAdapter`
