# PLAN: Task-62 — NotificationEvent 발행 패턴 통일 (패턴 A → 패턴 B)

## Branch
`ai/refactor-unify-notification-pattern` (from `main`)

## 목표
`NotificationEvent`를 트랜잭션 내 직접 발행하는 패턴 A 코드 2곳을 도메인 이벤트 기반 패턴 B로 통일한다.

---

## 현황 분석

### BuzzNotificationDispatcher
`@EventListener` (동기) → Buzz 서비스 트랜잭션 안에서 동기 실행되며 `NotificationEvent` 발행.
`NotificationEventListener`의 `AFTER_COMMIT`이 Buzz 트랜잭션 커밋까지 알림 실행을 미뤄줘 현재는 동작하지만,
Buzz 서비스 코드와 동기적으로 묶여 있어 패턴 불일치.

### FriendRequestReceiverActionService
`@Neo4jTransactional` 내에서 `NotificationEvent`를 직접 발행.
알림 제목(`"친구 수락"`), 알림 내용, `NotificationType`을 서비스 레이어가 직접 알고 있음 — 알림 인프라 관심사가 도메인 서비스에 누출.
`FriendshipCreatedEvent(userAId, userBId)`는 캐시 eviction 용도로 사용 중이라 알림용 필드 추가 불가.

### 테스트
`FriendRequestReceiverActionServiceTest:74` — `verify(eventPublisher).publishEvent(any(NotificationEvent.class))` 가 현재 검증하고 있어 수정 필요.

---

## 변경 파일 목록

### 신규 생성

**1. `social/domain/friend/event/FriendRequestAcceptedEvent.java`**
```java
public record FriendRequestAcceptedEvent(
        Long requesterId,
        Long receiverId,
        String receiverNickname
) {}
```

**2. `social/application/eventListener/FriendshipNotificationEventListener.java`**
```java
@Component
@RequiredArgsConstructor
public class FriendshipNotificationEventListener {
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        eventPublisher.publishEvent(new NotificationEvent(
                event.requesterId(),
                "친구 수락",
                event.receiverNickname() + "님과 이제 친구입니다.",
                NotificationType.FRIEND_REQUEST_ACCEPT,
                Map.of(
                        "friendId", event.receiverId(),
                        "friendName", event.receiverNickname()
                )
        ));
    }
}
```

### 수정

**3. `social/application/service/FriendRequestReceiverActionService.java`**
- `NotificationEvent` 직접 발행 제거
- `FriendRequestAcceptedEvent` 발행으로 대체:
```java
eventPublisher.publishEvent(new FriendRequestAcceptedEvent(
        request.getRequester().getId(),
        request.getReceiver().getId(),
        request.getReceiver().getNickname()
));
```
- `NotificationEvent`, `NotificationType` import 제거

**4. `buzz/application/eventListener/BuzzNotificationDispatcher.java`**
- 두 메서드 모두 `@EventListener` → `@Async` + `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 전환
- `ApplicationEventPublisher` import 유지 (발행 로직은 그대로)

### 테스트 수정

**5. `test/.../FriendRequestReceiverActionServiceTest.java`**
- line 74: `any(NotificationEvent.class)` → `any(FriendRequestAcceptedEvent.class)`
- `NotificationEvent` import 제거, `FriendRequestAcceptedEvent` import 추가

**6. `test/.../FriendshipNotificationEventListenerTest.java`** (신규)
- `FriendRequestAcceptedEvent` 수신 시 `NotificationEvent` 발행 검증
- 수신자 ID, 알림 제목, 알림 내용, metadata 검증

---

## 결정 사항

- **`@Transactional(REQUIRES_NEW)` 미적용**: `fallbackExecution = true` 이미 적용되어 있어 불필요. Flag 리스너들의 `REQUIRES_NEW`는 이전 workaround였으며 신규 리스너는 추가하지 않는다.
- **`FriendshipCreatedEvent` 변경 없음**: 캐시 eviction 전용으로 최소 필드 유지.
- **알림 문구는 리스너에**: 서비스에서 제거된 알림 제목/내용은 `FriendshipNotificationEventListener`로 이동.

---

## 예상 사이드 이펙트

- `FriendRequestReceiverActionService`가 더 이상 `NotificationEvent`를 직접 발행하지 않아 기존 테스트 1건 수정 필요.
- `BuzzNotificationDispatcher`의 `@EventListener` → `@Async` + `AFTER_COMMIT` 전환으로 Buzz 알림 발송 시점이 트랜잭션 커밋 이후로 변경 (기존에도 `NotificationEventListener`의 `AFTER_COMMIT`이 실질적으로 동일 효과를 냈으므로 사용자 체감 변화 없음).
