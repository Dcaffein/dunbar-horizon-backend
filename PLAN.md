# PLAN: UUID v7 전환 및 Outbox 이벤트 구조 단순화 (task-80)

## 작업 목표

1. 프로젝트 전체에서 `UUID.randomUUID()`를 `UuidUtil.createV7()`으로 통일한다.
2. `UserOutboxDomainEventListener`의 `registerAfterCommit()` 콜백 구조를 제거하고 Spring의 `@TransactionalEventListener(AFTER_COMMIT)` 메커니즘으로 대체한다.

---

## 현황 분석

### UUID v4 사용 위치 (5곳)

| 파일 | 라인 | 용도 |
|------|------|------|
| `account/domain/outbox/UserEventOutbox.java` | 47 | Outbox PK |
| `buzz/domain/Buzz.java` | 99 | 댓글 ID |
| `buzz/adapter/out/imageStorage/S3StorageAdapter.java` | 34 | S3 키 |
| `flag/application/service/flag/FlagSeedService.java` | 63 | 시드 group_id |
| `account/adapter/out/imageStorage/ProfileImageS3Adapter.java` | 30 | S3 키 |

### Outbox 이벤트 구조 문제

`registerAfterCommit()`은 `TransactionSynchronizationManager`에 콜백을 수동 등록하는 방식으로, `UserSyncIntegrationEvent`를 트랜잭션 외부에서 발행한다. 그 결과 `SocialUserEventListener`가 `@EventListener`를 써야 하는 제약이 생기고, 코드 의도 파악이 어렵다.

**변경 전:**
```
BEFORE_COMMIT → Outbox 저장 → registerAfterCommit() 콜백 등록
afterCommit() → UserSyncIntegrationEvent 발행 (트랜잭션 밖)
SocialUserEventListener: @EventListener (트랜잭션 무관)
```

**변경 후:**
```
BEFORE_COMMIT → Outbox 저장 → UserSyncIntegrationEvent 직접 발행 (트랜잭션 내)
SocialUserEventListener: @TransactionalEventListener(AFTER_COMMIT) (Spring이 커밋 후 지연 처리)
```

---

## 변경 파일 목록

### UUID 교체 (5개)
| 파일 | 변경 내용 |
|------|----------|
| `account/domain/outbox/UserEventOutbox.java` | `UUID.randomUUID()` → `UuidUtil.createV7()`, import 교체 |
| `buzz/domain/Buzz.java` | 동일 |
| `buzz/adapter/out/imageStorage/S3StorageAdapter.java` | 동일 |
| `flag/application/service/flag/FlagSeedService.java` | 동일 |
| `account/adapter/out/imageStorage/ProfileImageS3Adapter.java` | 동일 |

### Outbox 이벤트 구조 (2개)
| 파일 | 변경 내용 |
|------|----------|
| `account/application/eventListener/UserOutboxDomainEventListener.java` | `registerAfterCommit()` 제거, 각 핸들러에서 `UserSyncIntegrationEvent` 직접 발행, `TransactionSynchronization` 관련 import 제거 |
| `social/application/eventListener/SocialUserEventListener.java` | `@EventListener` → `@TransactionalEventListener(phase = AFTER_COMMIT)`, `@Neo4jTransactional` 추가 |

---

## 구현 방향

### UUID 교체

`UUID.randomUUID()` → `UuidUtil.createV7()`으로 단순 치환. `toString()` 호출이 필요한 곳은 `.toString()` 유지. `import java.util.UUID` 제거 후 `import com.example.DunbarHorizon.global.util.UuidUtil` 추가.

### Outbox 이벤트 구조

`onUserActivated` 예시:
```java
// 변경 전
UserEventOutbox outbox = outboxRepository.save(...);
registerAfterCommit(outbox, event.nickname(), event.profileImageUrl(), null);

// 변경 후
UserEventOutbox outbox = outboxRepository.save(...);
eventPublisher.publishEvent(new UserSyncIntegrationEvent(
        outbox.getId(), outbox.getAggregateId(), outbox.getEventType(),
        event.nickname(), event.profileImageUrl(), null
));
```

`registerAfterCommit()` 메서드 전체 삭제. `TransactionSynchronizationManager`, `TransactionSynchronization` import 제거.

`SocialUserEventListener`:
```java
// 변경 전
@Async
@EventListener
public void onUserSync(UserSyncIntegrationEvent event)

// 변경 후
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Neo4jTransactional   // findById + save를 단일 Neo4j 트랜잭션으로 묶어 원자성 확보
public void onUserSync(UserSyncIntegrationEvent event)
```

---

## 예상 사이드 이펙트

- `UserOutboxRetryService`는 `@Transactional` 내에서 `UserSyncIntegrationEvent`를 발행하므로, `SocialUserEventListener`의 `AFTER_COMMIT` 변경 후에도 정상 동작한다.
- 테스트 코드(`UserOutboxDomainEventListenerTest`, `SocialUserEventListenerTest`)에서 `@EventListener` 기반 검증 방식이 있다면 수정 필요.
