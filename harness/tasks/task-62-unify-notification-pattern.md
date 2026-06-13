# Task-62: NotificationEvent 발행 패턴 통일 (패턴 A → 패턴 B)

## Objective

`NotificationEvent`를 트랜잭션 내에서 직접 발행하는 패턴 A 코드를 도메인 이벤트 기반 패턴 B로 통일한다.

## Background

### 두 가지 발행 패턴이 혼재

**패턴 B (올바른 패턴):** 서비스는 도메인 이벤트만 발행, 별도 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` 리스너가 `NotificationEvent`로 변환
- `TraceEventListener`, `FlagDeletionEventListener`, `FlagEncoreEventListener`, `FlagInvitationEventListener`, `FlagMeetingChangedEventListener`

**패턴 A (개선 대상):** 서비스 또는 동기 리스너에서 `NotificationEvent`를 트랜잭션 내 직접 발행
- `FriendRequestReceiverActionService` — `@Neo4jTransactional` 내에서 알림 문구·타입을 서비스가 직접 알고 발행
- `BuzzNotificationDispatcher` — `@EventListener` (동기)로 `BuzzCreatedEvent`/`BuzzCommentedEvent`를 수신 후 `NotificationEvent` 발행

### 패턴 A의 문제
서비스 레이어가 알림 제목, 내용, 타입 등 알림 인프라 세부 사항을 알고 있다.
알림 문구 변경 시 서비스 코드를 수정해야 한다.

## Out of Scope

- `NotificationEventListener.fallbackExecution = true` 수정은 이미 완료 (task-62 이전 커밋)
- Flag 리스너들은 이미 패턴 B를 따르므로 변경 없음
- 알림 발송 로직(`NotificationEventListener`) 변경 없음

## Design Decisions

### FriendRequestReceiverActionService

`FriendshipCreatedEvent(userAId, userBId)`는 캐시 eviction에 쓰이는 최소 이벤트이므로 알림용 필드를 추가하지 않는다.

`FriendRequestAcceptedEvent(requesterId, receiverId, receiverNickname)` 이벤트를 신규 생성한다.
- `requesterId`: 알림 수신자 (친구 요청을 보낸 쪽)
- `receiverId`: 수락한 쪽 ID
- `receiverNickname`: 알림 문구 조립용

`FriendRequestReceiverActionService`에서 `NotificationEvent` 직접 발행을 제거하고 `FriendRequestAcceptedEvent`로 대체한다.

`FriendshipNotificationEventListener`(신규)가 `FriendRequestAcceptedEvent`를 수신해 `NotificationEvent`를 발행한다:
```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) { ... }
```

### BuzzNotificationDispatcher

`@EventListener` → `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 전환한다.
`BuzzCreatedEvent`, `BuzzCommentedEvent` 모두 Buzz 서비스 트랜잭션 내 `@DomainEvents`로 발행되므로
`AFTER_COMMIT`이 올바르게 동작한다.
트랜잭션 이후 발행 컨텍스트에서는 `fallbackExecution = true`(기등록)가 처리한다.
