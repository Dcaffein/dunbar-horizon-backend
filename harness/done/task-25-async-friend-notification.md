# Task 25: 친구 수락 Notification 비동기 분리

## Objective
`FriendEventListener.onFriendRequestAccepted()`에서 Friendship 생성과 Notification 발행을 분리한다.
Notification 실패가 Friendship 생성 트랜잭션에 영향을 주지 않도록, Notification 발행만 `@Async`로 처리한다.

## Background

### 현재 구조의 문제
`FriendEventListener`는 단일 메서드에서 두 가지 책임을 동기적으로 수행한다.

```java
@EventListener          // @TransactionalEventListener 가 아님 → 부모 트랜잭션 롤백 감지 불가
@Transactional
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    // 1. Friendship 생성 (중요 — 반드시 성공해야 함)
    Friendship friendship = friendshipBroker.establish(request);
    friendshipRepository.save(friendship);

    // 2. Notification 발행 (실패해도 Friendship에 영향 없어야 함)
    eventPublisher.publishEvent(notification);
}
```

문제점:
1. `@EventListener`를 사용해 부모 트랜잭션이 롤백돼도 이 핸들러는 이미 실행된 상태가 될 수 있다.
2. Notification 발행 실패(또는 이후 FCM 전송 실패) 시 `@Transactional`에 의해 Friendship 생성도 롤백된다.
3. 두 책임이 하나의 트랜잭션에 묶여 있어 Notification 지연이 요청 응답 시간에 직접 영향을 준다.

### LabelMemberShipEventListener는 변경 불필요
- social 도메인 내부 처리이며 Friendship 삭제와 동일 트랜잭션에서 라벨 정합성을 보장해야 하므로 동기 유지가 맞다.

## Domain Change
[ ] 없음  [x] 있음 (FriendEventListener 분리 → 클래스 추가)

## Decision

### 분리 방향
`FriendEventListener`를 두 개의 리스너로 분리한다.

**1. FriendEventListener** — Friendship 생성 (동기)
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    // Friendship 생성만 담당
}
```

**2. FriendNotificationEventListener** — Notification 발행 (비동기)
```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
    // NotificationEvent 발행만 담당
}
```

### 체크리스트
- [ ] `FriendEventListener`에서 Notification 발행 코드 제거, `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` 적용
- [ ] `FriendNotificationEventListener` 신규 생성 — `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
- [ ] `FriendEventListenerTest` 수정 — Friendship 생성 검증만 남김
- [ ] `FriendNotificationEventListenerTest` 신규 작성 — Notification 발행 검증

---

## Result

**브랜치:** `ai/refactor-async-friend-notification`
**커밋:** `c900688`

### 변경 내용

#### FriendEventListener (수정)
- `@EventListener` + `@Transactional` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`
- Friendship 생성만 전담, `ApplicationEventPublisher` 의존성 제거
- 부모 트랜잭션 커밋 후에만 실행 → Friendship이 부모 롤백에 영향받지 않음

#### FriendNotificationEventListener (신규)
- `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
- NotificationEvent 발행만 전담
- 비동기 실행으로 Notification 처리 지연이 응답 시간에 영향 없음
- Friendship 트랜잭션과 완전 분리

### 테스트 결과
- `FriendEventListenerTest`: 2/2 PASSED (Friendship 생성·예외 검증)
- `FriendNotificationEventListenerTest`: 1/1 PASSED (NotificationEvent 내용 검증)
