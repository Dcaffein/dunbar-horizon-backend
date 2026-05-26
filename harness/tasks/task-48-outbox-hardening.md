# Task-48: UserEventOutbox 안정성 보강

## 배경

Account-Social 동기화 구조 리뷰 중 `UserEventOutbox`에서 세 가지 취약점이 발견되었다.
현재 단일 인스턴스에서는 기능상 문제가 없으나, 운영 규모가 커지거나 다중 인스턴스 환경으로 전환될 때 데이터 정합성 및 성능 문제로 이어질 수 있다.

---

## 취약점 목록

### 1. `UserOutboxRetryScheduler`에 ShedLock 없음

`InteractionScoreFlushScheduler` 등 다른 스케줄러에는 `@SchedulerLock`이 적용되어 있으나,
`UserOutboxRetryScheduler`에는 적용되어 있지 않다.

다중 인스턴스 배포 시 여러 인스턴스가 동시에 동일한 PENDING 레코드를 집어가 중복 발행이 발생할 수 있다.
`onSyncCompleted`가 인메모리 Spring 이벤트에 의존하므로, 다른 인스턴스에서 처리된 경우
COMPLETED 마킹이 누락되어 MAX_RETRY 소진 후 FAILED로 전락할 수도 있다.

**수정:** `@SchedulerLock(name = "userOutboxRetry", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")` 추가

---

### 2. `user_event_outbox` 테이블 복합 인덱스 없음

retry 쿼리는 다음 조건을 사용한다.

```sql
WHERE status = 'PENDING' AND created_at < :threshold
```

현재 `UserEventOutbox` 엔티티에 `@Index` 선언이 없어 Hibernate가 인덱스를 생성하지 않는다.
테이블이 누적될수록 이 쿼리가 풀스캔으로 느려진다.

**수정:** `@Table`에 복합 인덱스 추가

```java
@Table(name = "user_event_outbox", indexes = {
    @Index(columnList = "status, createdAt")
})
```

---

### 3. COMPLETED / FAILED 레코드 정리 없음

이벤트가 성공하면 `COMPLETED`, 재시도 소진 시 `FAILED`로 상태가 바뀌지만
이 레코드들을 삭제하거나 아카이브하는 로직이 없다.
시간이 지날수록 테이블이 무한 증가하여 스토리지 및 쓰기 성능에 영향을 미친다.

**수정:** 일정 기간(예: 7일)이 지난 COMPLETED / FAILED 레코드를 삭제하는 정리 스케줄러 추가

```java
// 예시
@Scheduled(cron = "0 0 3 * * *")
@SchedulerLock(name = "outboxCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void cleanupProcessedOutbox() {
    outboxRepository.deleteProcessedOlderThan(LocalDateTime.now().minusDays(7));
}
```

---

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `UserOutboxRetryScheduler.java` | `@SchedulerLock` 추가 |
| `UserEventOutbox.java` | `@Table`에 `(status, createdAt)` 복합 인덱스 추가 |
| `UserEventOutboxJpaRepository.java` | `deleteProcessedOlderThan` 메서드 추가 |
| `UserEventOutboxRepository.java` (port) | `deleteProcessedOlderThan` 메서드 추가 |
| `UserEventOutboxRepositoryAdapter.java` | `deleteProcessedOlderThan` 구현 |
| `UserOutboxRetryService.java` (또는 신규 `UserOutboxCleanupService.java`) | 정리 로직 |
| `UserOutboxRetryScheduler.java` (또는 신규 `UserOutboxCleanupScheduler.java`) | 정리 스케줄러 |

## 브랜치

`ai/fix-outbox-hardening`

## 결과

- `UserOutboxRetryScheduler`에 `@SchedulerLock(name = "userOutboxRetry", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")` 추가
- `UserEventOutbox` `@Table`에 `(status, createdAt)` 복합 인덱스 추가
- `UserOutboxCleanupService` 신규 생성 — 7일 경과 COMPLETED/FAILED 레코드 배치 삭제
- `UserOutboxCleanupScheduler` 신규 생성 — 새벽 3시 ShedLock 기반 실행
- `UserOutboxCleanupServiceTest` 단위 테스트 작성
