# Task 16: Refactor Flag Exception Handling

## Objective
`FlagMemorial` 인가 예외와 `FlagComment` 조회 예외가 시스템 기본 예외로 처리되어 500 내부 서버 에러를 반환하는 문제를 수정한다. 이를 도메인 커스텀 예외 계층으로 편입하여 클라이언트에게 403(Forbidden) 및 404(Not Found) 상태 코드가 정상적으로 반환되도록 개선한다.

## Domain Change
[x] 있음

## Target Files
- `src/main/java/com/example/DunbarHorizon/flag/domain/memorial/exception/FlagMemorialAuthorizationException.java` — 예외 상속 구조 변경
- `src/main/java/com/example/DunbarHorizon/flag/application/service/comment/FlagCommentQueryService.java` — `orElseThrow()` 예외 명시

## Requirements
1. `FlagMemorialAuthorizationException`이 `RuntimeException` 대신 `FlagMemorialException`을 상속받도록 수정한다.
2. `FlagMemorialAuthorizationException`의 생성자에서 `super(message, HttpStatus.FORBIDDEN)`을 호출하여 403 상태 코드를 명시한다.
3. `FlagCommentQueryService.java`의 `getCommentTree()` 메서드 내에서 `flagRepository.findById(flagId).orElseThrow()`를 호출하는 부분을 `orElseThrow(() -> new FlagNotFoundException(flagId))`로 수정한다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| Memorial 인가 예외 타입 | `FlagMemorialException` 상속 | 403 FORBIDDEN 상태 코드 매핑 |
| Comment 조회 실패 예외 | `FlagNotFoundException` | 기존 서비스 계층 일관성 유지 |