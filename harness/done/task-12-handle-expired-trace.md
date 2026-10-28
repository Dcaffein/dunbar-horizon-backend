## Objective
만료된 Trace에 접근할 때 발생하는 서버 에러(500)를 없애고, 도메인 정책에 맞추어 과거의 발자국 기록을 자동으로 초기화(Reset)하여 새로운 상호작용을 시작할 수 있게 한다.

## Domain Change
[x] 있음

## Target Files
- `{trace}/domain/model/Trace.java` — 엔티티 내부 만료 처리 로직 수정
- `{trace}/domain/model/TraceTest.java` — 도메인 단위 테스트 수정

## Requirements
1. `Trace` 도메인의 `recordVisit` 메서드에서 만료 여부(`isExpired`)가 참일 때 발생하던 `IllegalStateException` 제거.
2. 예외를 던지는 대신, 과거의 방문 카운트, 시간, 공개 상태 등을 모두 초기화하는 `resetTrace()` 내부 메서드를 호출하도록 수정한다.
3. 초기화 후 현재 방문자의 새로운 카운트가 1부터 다시 적재되도록 흐름을 이어간다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 초기화 범위 | userACount, userBCount, userALastVisitedAt, userBLastVisitedAt, isRevealed, revealedAt, lastTracedAt(현재 시각) | 이벤트 큐(`clearDomainEvents()`) 포함 |