# Task: FriendshipBroker 리팩터링 및 친구 수락 흐름 직접 처리로 전환

## Background

현재 친구 수락 흐름에 두 가지 문제가 있다.

**문제 1: 어정쩡한 이벤트 설계 (실제 버그)**

`FriendRequestReceiverActionService.acceptRequest()`는 FriendRequest를 **삭제한 뒤** `FriendRequestAcceptedEvent`를 발행한다.
`FriendEventListener`는 이 이벤트를 받아 FriendRequest를 **다시 조회**하려 하지만, 이미 삭제되어 `FriendRequestNotFoundException`이 발생한다.
또한 `friendshipBroker.establish(request)`가 서비스와 리스너 양쪽에서 호출되지만, 서비스의 반환값은 무시된다.

**문제 2: FriendshipBroker 설계 어색함**

- `establish(FriendRequest)`는 메서드명이 파라미터와 어울리지 않는다.
- `establish()` 내부의 null 체크는 FriendRequest 불변식을 브로커가 떠맡는 단일 책임 위반이다.
- `propose()`의 자기 자신 체크(`requester == receiver`)는 FriendRequest 객체 불변식이므로 생성자에 있어야 한다.

## Objective

- 친구 수락 흐름을 이벤트 없이 직접 처리(방향 A)로 전환한다.
- `FriendshipBroker`의 역할을 명확히 한다: 크로스-애그리거트 검증 + Friendship/FriendRequest 생성.
- 알림은 기존대로 `NotificationEvent`로 발행한다 (notification 도메인 경계 유지).

## Domain Change

[ ] 없음  [x] 있음

- `FriendRequest` — 생성자에 자기 자신 체크 추가
- `FriendshipBroker` — `propose()`에서 자기 자신 체크 제거, `establish()` → `createFrom()`으로 리네임 및 null 체크 제거
- `FriendRequestReceiverActionService` — Friendship 저장 직접 처리, `FriendRequestAcceptedEvent` 대신 `NotificationEvent` 직접 발행
- `FriendEventListener` — 삭제
- `FriendRequestAcceptedEvent` — 삭제

## Decision

### FriendRequest 생성자

```java
// 추가
public FriendRequest(UserReference requester, UserReference receiver) {
    if (requester.getId().equals(receiver.getId())) {
        throw new CannotRequestToSelfException(requester.getId());
    }
    this.requester = requester;
    this.receiver = receiver;
    // ...
}
```

### FriendshipBroker.propose()

```java
// 제거: self-check (생성자로 이동)
// 유지: existsFriendshipBetween(), existsRequestBetween()
public FriendRequest propose(UserReference requester, UserReference receiver) {
    if (friendshipRepository.existsFriendshipBetween(requesterId, receiverId)) { ... }
    if (friendRequestRepository.existsRequestBetween(requesterId, receiverId)) { ... }
    return new FriendRequest(requester, receiver);
}
```

### FriendshipBroker.createFrom() (구 establish)

```java
// 제거: null 체크
// 유지: isAccepted() 체크, existsFriendshipBetween() 체크
public Friendship createFrom(FriendRequest friendRequest) {
    if (!friendRequest.isAccepted()) { ... }
    if (friendshipRepository.existsFriendshipBetween(...)) { ... }
    return new Friendship(friendRequest.getRequester(), friendRequest.getReceiver());
}
```

### FriendRequestReceiverActionService.acceptRequest()

```java
public void acceptRequest(String requestId, Long receiverId) {
    FriendRequest request = findRequestById(requestId);
    request.accept(receiverId);

    Friendship friendship = friendshipBroker.createFrom(request);
    friendshipRepository.save(friendship);
    friendRequestRepository.deleteById(requestId);

    eventPublisher.publishEvent(new NotificationEvent(
        request.getRequester().getId(),
        "친구 수락",
        request.getReceiver().getNickname() + "님과 이제 친구입니다.",
        NotificationType.FRIEND_REQUEST_ACCEPT,
        Map.of("friendId", request.getReceiver().getId(), ...)
    ));
}
```

## Checklist

- [ ] FriendRequest 생성자 자기 자신 체크 추가
- [ ] FriendshipBroker.propose() 자기 자신 체크 제거
- [ ] FriendshipBroker.establish() → createFrom() 리네임 및 null 체크 제거
- [ ] FriendRequestReceiverActionService에 FriendshipRepository 주입 및 직접 저장
- [ ] FriendRequestReceiverActionService에서 NotificationEvent 직접 발행
- [ ] FriendEventListener 삭제
- [ ] FriendRequestAcceptedEvent 삭제

## Result

- 브랜치:
- 커밋:
- 변경 파일:
