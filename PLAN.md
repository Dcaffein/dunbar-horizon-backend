# PLAN: Task-48 UserEventOutbox 안정성 보강

## 작업 목표

Account-Social 동기화 구조 리뷰에서 발견된 `UserEventOutbox`의 세 가지 취약점을 수정한다.

---

## 현황 분석

| 취약점 | 현재 상태 | 문제 |
|--------|-----------|------|
| ShedLock | `UserOutboxRetryScheduler`에 `@SchedulerLock` 없음 | 다중 인스턴스 환경에서 동일 PENDING 레코드 중복 처리 가능 |
| 복합 인덱스 | `UserEventOutbox` 엔티티에 `@Index` 없음 | `WHERE status = PENDING AND createdAt < threshold` 쿼리가 테이블 풀스캔 |
| 레코드 정리 | COMPLETED/FAILED 레코드 삭제 로직 없음 | 테이블 무한 증가 → 스토리지·쓰기 성능 저하 |

---

## 변경 파일 목록

### 수정

| 파일 | 변경 내용 |
|------|-----------|
| `UserOutboxRetryScheduler.java` | `@SchedulerLock(name = "userOutboxRetry", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")` 추가 |
| `UserEventOutbox.java` | `@Table`에 `@Index(columnList = "status, createdAt")` 추가 |
| `UserEventOutboxJpaRepository.java` | `deleteByStatusInAndCreatedAtBefore` 추가 (`@Modifying`) |
| `UserEventOutboxRepository.java` (port) | `deleteProcessedOlderThan(LocalDateTime threshold)` 추가 |
| `UserEventOutboxRepositoryAdapter.java` | `deleteProcessedOlderThan` 구현 |

### 신규 생성

| 파일 | 내용 |
|------|------|
| `UserOutboxCleanupService.java` | RETENTION_DAYS=7 기준 COMPLETED/FAILED 레코드 삭제 |
| `UserOutboxCleanupScheduler.java` | `cron = "0 0 3 * * *"` + `@SchedulerLock(name = "userOutboxCleanup")` |

---

## 구현 방향

### ShedLock 파라미터 근거

- `lockAtMostFor = "PT5M"`: 스케줄러 자체 interval과 동일 — 비정상 종료 시 최대 5분 후 잠금 해제
- `lockAtLeastFor = "PT1M"`: 빠르게 끝나도 중복 실행 방지를 위한 최소 보유 시간

### 정리 스케줄러 타이밍

- `cron = "0 0 3 * * *"` (새벽 3시) — FriendshipDecayScheduler(3시)와 동일 시간대지만 각자 ShedLock name이 다르므로 충돌 없음
- RETENTION_DAYS = 7: 운영 이슈 트래킹을 위한 최소 보존 기간

### deleteProcessedOlderThan 쿼리

COMPLETED와 FAILED를 한 번에 처리하기 위해 `IN :statuses` 사용.
JPQL `DELETE` + `@Modifying`으로 배치 삭제 (엔티티 로딩 없이 직접 DELETE 쿼리 실행).

---

## 예상 사이드 이펙트

- `@Index` 추가는 DDL 변경이므로 `spring.jpa.hibernate.ddl-auto`가 `validate`인 프로덕션 환경에서는 Flyway/Liquibase 마이그레이션 스크립트가 별도로 필요하다.
- 그 외 기존 동작에 영향 없음.

---

## 테스트 전략

**단위 테스트** (`UserOutboxCleanupServiceTest`, MockitoExtension)

- `cleanupProcessed_정리대상없으면_deleteNotCalled`: RETENTION_DAYS 이내 레코드만 있을 때 delete 미호출 검증
- `cleanupProcessed_7일지난레코드_deleteHo출`: threshold 계산 및 `deleteProcessedOlderThan` 위임 검증
