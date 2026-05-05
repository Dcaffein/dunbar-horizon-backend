# PLAN — FriendRequest DB 유니크 제약 조건 추가

## 작업 목표

`FriendRequest` 노드에 Neo4j DB 레벨 유니크 제약 조건을 추가하여,
도메인 체크(check-then-act)를 통과하는 동시 요청을 최종 방어한다.

## 배경

`FriendshipBroker.propose()`의 중복 검사는 순차 요청의 정상 흐름을 처리하지만,
두 트랜잭션이 동시에 체크를 통과하는 TOCTOU 레이스 컨디션을 막지 못한다.
DB 레벨 유니크 제약이 이 케이스를 방어하는 최종 안전망이 된다.

---

## 수정·생성 파일

| 구분 | 파일 |
|------|------|
| 수정 | `social/domain/friend/FriendRequest.java` |
| 생성 | `social/adapter/out/neo4j/schema/FriendRequestSchemaInitializer.java` |
| 수정 | `social/application/service/FriendRequesterActionService.java` |
| 수정 | `social/application/FriendRequesterActionServiceTest.java` |

---

## 구현 상세

### 1. `FriendRequest` — `pairKey` 추가

`Friendship.generateCompositeId()`와 동일한 방식으로 `min(A,B)_max(A,B)` 형태의
`pairKey` 프로퍼티를 추가하고 생성자에서 세팅한다.

```java
@Property
private String pairKey;   // e.g. "1_2"

FriendRequest(UserReference requester, UserReference receiver) {
    ...
    long min = Math.min(requester.getId(), receiver.getId());
    long max = Math.max(requester.getId(), receiver.getId());
    this.pairKey = min + "_" + max;
}
```

### 2. `FriendRequestSchemaInitializer` — 신규 생성

`SocialUserSchemaInitializer`와 동일한 패턴으로 작성한다.

```java
neo4jClient.query(
    "CREATE CONSTRAINT friend_request_pair_unique IF NOT EXISTS " +
    "FOR (r:FriendRequest) REQUIRE r.pairKey IS UNIQUE"
).run();
```

`IF NOT EXISTS`로 멱등하게 동작하므로 인스턴스 재시작 시 에러가 발생하지 않는다.

### 3. `FriendRequesterActionService` — 제약 위반 처리

동시 요청이 DB 제약에 걸릴 경우 Spring이
`DataIntegrityViolationException`을 던진다.
이를 catch하여 `DuplicateFriendRequestException`으로 변환한다.

```java
try {
    return friendRequestRepository.saveRequest(newRequest);
} catch (DataIntegrityViolationException e) {
    throw new DuplicateFriendRequestException(requesterId, receiverId);
}
```

---

## 테스트 전략

`FriendRequesterActionServiceTest` (기존 파일 수정, 단위 테스트):
- `sendRequest` 시 `friendRequestRepository.saveRequest()`가
  `DataIntegrityViolationException`을 던질 때
  `DuplicateFriendRequestException`으로 변환되는지 검증

---

## 예상 사이드 이펙트

- 기존 Neo4j에 `pairKey`가 없는 `FriendRequest` 노드가 존재하면
  제약 조건 생성이 실패할 수 있다. (개발 환경 DB 초기화 권장)
- `FriendRequest` 생성자가 package-private이므로 외부 변경 영향 없음.
