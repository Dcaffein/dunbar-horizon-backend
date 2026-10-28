## Objective
두 사용자가 동시에 서로의 프로필을 처음 방문할 때 발생하는 `INSERT` 충돌(Race Condition)로 인한 500 에러를 방지하고 데이터 무결성을 보장한다.

## Domain Change
[ ] 없음

## Target Files
- `{trace}/application/TraceService.java` — `@Retryable` 적용
- `global/config/` (또는 메인 클래스) — `@EnableRetry` 활성화

## Background: 왜 inline try-catch가 아닌가
`DataIntegrityViolationException`은 JPA flush 시점에 발생하며, 이 순간 Hibernate 세션이 broken 상태가 된다.
같은 트랜잭션 안에서 예외를 catch하더라도 이후 JPA 작업이 불가능하므로, 재조회·재시도 로직을 동일 트랜잭션 내에서 실행할 수 없다.

## Requirements
1. `TraceService.recordTrace()`에 `@Retryable`을 적용한다.
   - `retryFor`: `DataIntegrityViolationException.class`, `OptimisticLockingFailureException.class`
   - `maxAttempts`: 3
2. `@EnableRetry`를 프로젝트에 활성화한다.
3. 재시도 시 동작은 기존 흐름을 그대로 따른다.
   - 재시도 1회차: `findByUserAIdAndUserBId` → 상대 트랜잭션이 이미 INSERT한 row 조회 → `recordVisit` → UPDATE
   - `@Version` 필드가 동시 UPDATE 충돌을 `OptimisticLockingFailureException`으로 변환 → 추가 재시도로 처리

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 재시도 전략 | `@Retryable` (Spring Retry) | AOP outer proxy → 재시도 시 새 트랜잭션 보장 |
| 포착 예외 | `DataIntegrityViolationException`, `OptimisticLockingFailureException` | INSERT 충돌 + 동시 UPDATE 충돌 모두 처리 |
| 최대 재시도 횟수 | 3 | 1회 INSERT 충돌 + 1회 UPDATE 충돌 여유 |
| 프록시 순서 | `@Retryable` (outer) → `@Transactional` (inner) | Spring Retry 기본 order가 @Transactional보다 높음 |