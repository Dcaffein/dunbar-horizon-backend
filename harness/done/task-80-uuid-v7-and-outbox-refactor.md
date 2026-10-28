# Task-80: UUID v7 전환 및 Outbox 이벤트 구조 단순화

## 배경 및 목적

두 가지 코드 품질 문제를 함께 해결한다.

1. **UUID v4 → v7 전환**: 일부 도메인에서 `UUID.randomUUID()`(v4)를 직접 사용하고 있어 프로젝트 공통 유틸인 `UuidUtil.createV7()`과 불일치한다. UUID v7은 시간 기반으로 정렬 가능하여 DB 인덱스 성능에 유리하다.

2. **Outbox 이벤트 발행 구조 단순화**: `UserOutboxDomainEventListener`의 `registerAfterCommit()` 메서드는 `TransactionSynchronizationManager`에 콜백을 수동으로 등록하는 복잡한 구조다. `@TransactionalEventListener(BEFORE_COMMIT)`에서 통합 이벤트를 직접 발행하고 Social 쪽 리스너를 `@TransactionalEventListener(AFTER_COMMIT)`으로 변경하면 Spring이 제공하는 방식으로 동일한 보장(커밋 후 발행)을 더 직관적으로 얻을 수 있다.

## 의도

### UUID v7 전환 대상 (5곳)

| 파일 | 위치 |
|------|------|
| `account/domain/outbox/UserEventOutbox.java` | `pending()` 팩토리 메서드 |
| `buzz/domain/Buzz.java` | `createComment()` 내 commentId 생성 |
| `buzz/adapter/out/imageStorage/S3StorageAdapter.java` | presign 키 생성 |
| `flag/application/service/flag/FlagSeedService.java` | 시드 데이터 group_id 생성 |
| `account/adapter/out/imageStorage/ProfileImageS3Adapter.java` | presign 키 생성 |

모두 `UUID.randomUUID()` → `UuidUtil.createV7()`으로 교체하고 `import java.util.UUID`를 제거한다.

### Outbox 이벤트 구조 단순화

**변경 전 (`UserOutboxDomainEventListener`):**
```
@TransactionalEventListener(BEFORE_COMMIT)
    → Outbox 저장
    → registerAfterCommit() 콜백 등록
        → afterCommit() 시점에 UserSyncIntegrationEvent 발행

SocialUserEventListener: @EventListener (트랜잭션 무관)
```

**변경 후:**
```
@TransactionalEventListener(BEFORE_COMMIT)
    → Outbox 저장
    → UserSyncIntegrationEvent 직접 발행 (outboxId 페이로드에 포함)

SocialUserEventListener: @TransactionalEventListener(AFTER_COMMIT) + @Async
    → Spring이 커밋 후 발행을 자동 지연 처리
```

`registerAfterCommit()` 메서드와 `TransactionSynchronizationManager` 의존을 제거한다.

## Out of Scope

- Outbox 재시도 로직(`UserOutboxRetryService`) 변경 없음
  - 재시도 경로는 `@Transactional` 내에서 발행하므로 `@TransactionalEventListener(AFTER_COMMIT)`이 정상 동작함
- `SocialUserEventListener`의 Neo4j 처리 로직 변경 없음
