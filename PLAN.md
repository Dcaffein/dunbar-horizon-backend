# PLAN.md — Task 03: Transactional Outbox + Account→Social 동기화

## 목표

현재 `SocialUserEventListener`의 `@Async` + `BEFORE_COMMIT` 패턴을 제거하고, Transactional Outbox 패턴을 도입하여 Account→Social 이벤트를 유실 없이 전달한다.

---

## 현재 문제 분석

### 버그: `SocialUserEventListener`
```
@Async                                             // 별도 스레드 실행
@TransactionalEventListener(phase = BEFORE_COMMIT) // 현재 트랜잭션 컨텍스트 필요
```
`@Async`로 분리된 스레드는 원래 JPA 트랜잭션과 독립적이므로, Neo4j 쓰기가 account 트랜잭션과 원자성을 보장받지 못한다.

### 현재 이벤트 발행 경로 (두 가지)
1. **도메인 이벤트 경로**: `User.activate()`/`deactivate()` → `registerEvent()` → `userJpaRepository.save(user)` → Spring Data `@DomainEvents` → `ApplicationEventPublisher`
2. **직접 발행 경로**: `SignupService.registerOAuthUser()` → `eventPublisher.publishEvent(new UserActivatedEvent(...))` 직접 호출

두 경로 모두 `ApplicationEventPublisher`를 통해 발행되므로, `OutboxDomainEventListener`가 동일하게 수신 가능하다.

---

## 이벤트 흐름 설계

### 즉시 발행 경로 (Happy Path)

```
User.activate() / User.deactivate()
        ↓
UserActivatedEvent / UserDeactivatedEvent (Domain Event)
        ↓
OutboxDomainEventListener — @TransactionalEventListener(BEFORE_COMMIT), 동기
        ├── Outbox 레코드 저장 (status=PENDING)  ← JPA 트랜잭션에 포함
        └── TransactionSynchronizationManager.registerSynchronization()
                             ↓ (트랜잭션 커밋 후)
                    UserSyncIntegrationEvent 발행
                             ↓
        SocialUserEventListener — @EventListener, @Async
                ├── Neo4j SocialUser 생성/활성화/비활성화
                └── 성공: Outbox status → COMPLETED (새 @Transactional 호출)
                    실패: PENDING 유지 (Scheduler가 재시도)
```

### 재시도 경로 (Scheduler)

```
OutboxRetryScheduler — @Scheduled(fixedDelay = 5분)
        ↓
PENDING 레코드 중 createdAt < now - 5분 조회
        ↓
OutboxRetryService.retry(outbox)  — @Transactional
        ├── retry_count > 5 → status = FAILED + ERROR 로그
        └── retry_count ≤ 5 → retry_count++ 저장 → UserSyncIntegrationEvent 발행
                             ↓
               SocialUserEventListener (@EventListener, @Async)
```

> **설계 근거**: `SocialUserEventListener`를 `@EventListener`(비트랜잭셔널)로 변경하면, 즉시 발행 경로(`TransactionSynchronizationManager.afterCommit()`)와 스케줄러 재시도 경로(트랜잭션 무관) 모두에서 동일한 코드로 처리 가능하다.

---

## 생성/수정 파일 목록

### 신규 생성 (11개)

| 파일 경로 | 역할 |
|-----------|------|
| `account/domain/outbox/UserEventOutbox.java` | Outbox JPA 엔티티 (UUID PK, aggregateId, eventType, payload JSON, status, retryCount, createdAt, processedAt) |
| `account/domain/outbox/UserOutboxEventType.java` | enum: ACTIVATE, DEACTIVATE |
| `account/domain/outbox/UserOutboxStatus.java` | enum: PENDING, COMPLETED, FAILED |
| `account/domain/outbox/repository/UserEventOutboxRepository.java` | Repository 인터페이스 (save, findById, findPendingOlderThan) |
| `account/adapter/out/persistence/jpa/UserEventOutboxJpaRepository.java` | Spring Data JPA |
| `account/adapter/out/persistence/UserEventOutboxRepositoryAdapter.java` | Repository 어댑터 구현 |
| `global/event/user/UserSyncIntegrationEvent.java` | Integration Event record (outboxId, userId, eventType, nickname, profileImageUrl) |
| `account/application/eventHandler/UserOutboxDomainEventListener.java` | BEFORE_COMMIT: Outbox 저장 + AFTER_COMMIT 콜백 등록 |
| `account/application/service/UserOutboxRetryService.java` | 재시도/Dead Letter 처리, Outbox 상태 업데이트 |
| `account/adapter/in/scheduler/UserOutboxRetryScheduler.java` | @Scheduled 5분 주기 재시도 트리거 |

> Dead Letter 알림 채널은 미결정 상태로, 5회 초과 FAILED 전환 시 ERROR 로그만 남기고 알림은 추후 연동한다.

### 수정 (2개)

| 파일 경로 | 변경 내용 |
|-----------|---------|
| `social/application/eventHandler/SocialUserEventListener.java` | `UserSyncIntegrationEvent` 수신, `@EventListener` + `@Async`로 변경, Outbox COMPLETED 마킹 추가 |

### 변경 불필요 (확인 완료)

| 파일 | 이유 |
|------|------|
| `VerificationService.java` | `user.activate()` → `@DomainEvents` 경로로 `UserActivatedEvent` 자동 발행됨 |
| `SignupService.java` | `eventPublisher.publishEvent()` 직접 발행으로 `OutboxDomainEventListener` 수신 가능 |
| `JpaConfig.java` | `account` 패키지가 이미 JPA base packages에 포함됨 |

---

## 테스트 계획

| 테스트 클래스 | 유형 | 검증 항목 |
|--------------|------|---------|
| `UserOutboxDomainEventListenerTest` | 단위 (Mockito) | BEFORE_COMMIT 시 Outbox PENDING 저장 |
| `UserEventOutboxRepositoryTest` | 슬라이스 (JpaRepositoryTest 상속) | PENDING 저장/조회/상태 변경 쿼리 |
| `UserOutboxRetryServiceTest` | 단위 (Mockito) | retry_count 증가, 5회 초과 시 FAILED 전환 + Slack 호출 |
| `SocialUserEventListenerTest` | 단위 (Mockito) | Integration Event 수신 → Neo4j 처리 + Outbox COMPLETED |

---

## 예상 사이드 이펙트

1. **`SocialUserEventListener` 변경**: `UserActivatedEvent`/`UserDeactivatedEvent`를 직접 리스닝하는 코드가 이 리스너뿐이므로 영향 범위 없음 (확인 완료).
3. **Outbox 테이블 추가**: MySQL 스키마에 `user_event_outbox` 테이블이 추가된다. `ddl-auto=update` 환경에서는 자동 생성되나, `prod`에서는 수동 DDL 적용이 필요하다.

---

## 브랜치

`ai/feat-sync-outbox`
