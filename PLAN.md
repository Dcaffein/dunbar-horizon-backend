# PLAN: Task-25 — 친구 수락 Notification 비동기 분리

## [도메인 수정 승인 요청]

`FriendNotificationEventListener` 신규 클래스 생성 (`social/application/eventListener/` 하위).

---

## 작업 목표

`FriendEventListener.onFriendRequestAccepted()`에서 Friendship 생성과 Notification 발행을 두 개의 리스너로 분리한다.
Notification 실패가 Friendship 생성에 영향을 주지 않도록, Notification 발행만 `@Async`로 처리한다.

---

## 현황 분석

`FriendEventListener` (현재 30-50줄):
```java
@EventListener            // ← 부모 트랜잭션 롤백을 감지하지 못함
@Transactional
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    // 1. Friendship 생성
    friendshipRepository.save(friendshipBroker.establish(request));

    // 2. Notification 발행  ← 실패 시 Friendship까지 롤백됨
    eventPublisher.publishEvent(notification);
}
```

**문제:**
1. `@EventListener`는 부모 트랜잭션 컨텍스트 밖에서 실행 — 부모 롤백과 무관하게 실행됨
2. Notification 발행 실패 시 `@Transactional`에 의해 Friendship 생성도 함께 롤백
3. Notification 처리 지연이 응답 시간에 직접 영향

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `social/application/eventListener/FriendEventListener.java` | `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)`, `@Transactional` → `REQUIRES_NEW`, Notification 코드·`ApplicationEventPublisher` 제거 |
| `social/application/eventListener/FriendNotificationEventListener.java` | **신규** — `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`, Notification 발행 전담 |
| `social/application/FriendEventListenerTest.java` | Notification 검증 제거, `eventPublisher` mock 제거 |
| `social/application/FriendNotificationEventListenerTest.java` | **신규** — Notification 발행 내용 검증 |

---

## 구현 방향

### 1. FriendEventListener (Friendship 생성 전담)

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    FriendRequest request = friendRequestRepository.findById(event.requestId())
            .orElseThrow(() -> new FriendRequestNotFoundException(event.requestId()));
    friendshipRepository.save(friendshipBroker.establish(request));
}
```

- `ApplicationEventPublisher` 필드 제거
- `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)`: 부모 트랜잭션이 커밋된 후에만 실행
- `@Transactional` → `REQUIRES_NEW`: Friendship 생성을 독립 트랜잭션으로 격리

### 2. FriendNotificationEventListener (Notification 발행 전담, 신규)

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    NotificationEvent notification = new NotificationEvent(
            event.requesterId(), "친구 수락",
            event.receiverNickname() + "님과 이제 친구입니다.",
            NotificationType.FRIEND_REQUEST_ACCEPT,
            Map.of("friendId", event.receiverId(), "friendName", event.receiverNickname())
    );
    eventPublisher.publishEvent(notification);
}
```

- `@Async`: 비동기 실행 — Notification 처리가 응답 시간에 영향 없음
- `@TransactionalEventListener(AFTER_COMMIT)`: 부모 커밋 후 실행
- `@Transactional` 없음: NotificationEvent 단순 발행이므로 별도 트랜잭션 불필요

---

## 예상 사이드 이펙트

- Friendship 생성과 Notification 발행이 각각 독립적으로 실행 — Notification 실패가 Friendship에 영향 없음
- `FriendEventListener`에서 `ApplicationEventPublisher` 의존성 제거

---

## 테스트 전략

- `FriendEventListenerTest`: Friendship 생성만 검증, `eventPublisher` mock 제거, Notification 검증 제거
- `FriendNotificationEventListenerTest` (신규): NotificationEvent가 올바른 내용으로 발행되는지 검증
