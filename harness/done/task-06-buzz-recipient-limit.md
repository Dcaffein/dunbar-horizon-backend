## Objective
비정상적으로 거대한 수신자 배열 주입을 차단하여 Social DB(Neo4j)와 Buzz DB(MongoDB)의 부하를 방지하고 시스템 가용성을 보호합니다.

## Domain Change
[x] 있음 — `Buzz` 도메인 내 수신자 수 불변식(Invariant) 검증 로직 추가

## Target Files
- `buzz/adapter/in/web/dto/ManualRecipientRequest.java` — JSR-380 검증 추가
- `buzz/domain/Buzz.java` — 생성 시 크기 검증 로직 추가

## Requirements
1. `ManualRecipientRequest` DTO의 `memberIds` 필드에 `@Size(max = 150)` 어노테이션을 적용하여 API 진입점에서 Reject 처리(Fail-fast)합니다.
2. `Buzz` 도메인 엔티티 생성자에서 수신자 목록의 크기가 150명을 초과할 경우 예외를 발생시켜, 생성 경로와 무관하게 도메인 무결성을 보호합니다.
3. 150명 초과 시 컨트롤러에서는 `MethodArgumentNotValidException`, 도메인에서는 `BuzzInvalidStateException`이 발생하도록 처리합니다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 최대 수신자 수 | 150 | 던바의 수(Dunbar's Number) 기준 |
| 검증 위치 | DTO 및 Domain 이중 검증 | API 방어 및 도메인 캡슐화 |
| 예외 타입 | BuzzInvalidStateException | 기존 도메인 예외 재사용 |