**Description**
Account 모듈의 상태 변경 이벤트를 유실 없이 Social 모듈로 전달하기 위해 Transactional Outbox 패턴을 도입
기존 `SocialUserEventListener`의 결합도를 낮추고 데이터 정합성을 보장

**Prerequisites**
- 없음

**Technical Specification**
- **이벤트 전파 방식:** Spring의 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 및 `ApplicationEventPublisher`를 활용하여 
    동일 JVM 내에서 이벤트를 비동기로 전달.
- **스키마 (user_event_outbox):**
    - `id` (VARCHAR/UUID, PK)
    - `aggregate_id` (VARCHAR, 대상 유저 ID)
    - `event_type` (VARCHAR, 예: SIGNUP, ACTIVATE, DEACTIVATE)
    - `payload` (JSON/TEXT, 이벤트 세부 데이터)
    - `status` (VARCHAR, PENDING / COMPLETED / FAILED)
    - `retry_count` (INT, 기본값 0)
    - `created_at` (TIMESTAMP)
    - `processed_at` (TIMESTAMP, NULL 허용)
- **Dead Letter 채널:** `retry_count`가 5회를 초과하여 FAILED 상태가 된 경우, 애플리케이션 로그에 ERROR 레벨로 적재. (현재 알람 발송 채널은 아직 미정)
- **기존 코드 리팩토링:** 기존 `SocialUserEventListener`는 도메인 이벤트를 직접 수신하지 않고, Outbox 스케줄러나 즉시 발행 로직이 던지는 통합 이벤트(Integration Event)를 수신하도록 변경.

**Acceptance Criteria**
1. Account 모듈에서 유저 생성 시, 동일 트랜잭션 내에 Outbox 테이블에 PENDING 상태의 레코드가 생성되어야 한다.
2. 트랜잭션 커밋 직후 이벤트가 발행되어 Social 모듈이 수신하면 Outbox 레코드 상태가 COMPLETED로 변경되어야 한다.
3. Social 모듈 처리 중 예외 발생 시 Outbox 상태는 PENDING으로 유지되어야 한다.
4. 스케줄러(5분 주기)가 생성된 지 5분이 지난 PENDING 레코드를 재발행해야 한다.
5. 스케줄러로 재시도하는 이벤트 역시 서비스가 아닌 publish를 사용해야한다.
6. 재시도 횟수가 5회를 초과한 레코드는 FAILED 상태로 변경되고 ERROR 로그가 발생해야 한다.