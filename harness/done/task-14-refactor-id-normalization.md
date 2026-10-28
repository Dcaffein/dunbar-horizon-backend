## Objective
서비스와 어댑터 양쪽에 흩어져 있던 ID 정렬(`Math.min/max`) 로직을 도메인 계층 단일 불변식으로 응집시킨다. 정렬 방식은 `Trace`의 static 메서드 뒤에 캡슐화하여 호출자가 정렬 구현을 알 필요 없도록 한다.

## Domain Change
[x] 있음

## Target Files
- `{trace}/domain/model/Trace.java` — static 메서드 및 반환 타입 레코드 추가
- `{trace}/application/TraceService.java` — 정렬 로직 제거
- `{trace}/adapter/out/persistence/TraceRepositoryAdapter.java` — static 메서드 사용

## Requirements
1. `Trace` 도메인 내부에 `TraceIdPair` 레코드(`minId`, `maxId`)를 선언한다.
2. `Trace`에 `public static TraceIdPair sortIds(Long id1, Long id2)` static 메서드를 추가한다. 항상 작은 값이 `minId`가 되도록 정렬하여 반환한다.
3. `Trace` 생성자는 `sortIds()`를 통해 정렬된 값을 할당하도록 수정한다.
4. `TraceService`에 존재하던 `Math.min/max` 로직을 완전히 삭제한다.
5. `TraceRepositoryAdapter`는 DB를 조회하기 전, `Trace.sortIds()`를 호출하여 정렬한 뒤 쿼리를 실행한다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 캡슐화 단위 | `Trace.sortIds(Long id1, Long id2)` static 메서드 | 호출자는 정렬 방식을 알 필요 없음 |
| 반환 타입 | `public record TraceIdPair(Long minId, Long maxId)` | `Trace` 내부 중첩 선언 |