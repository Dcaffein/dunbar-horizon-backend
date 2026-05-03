## Objective
`capacity`가 null(무제한)인 Flag에 참여 시도 시 `FlagFullCapacityException`이 발생하는 논리 오류를 수정한다.

## Domain Change
[x] 있음

## Target Files
- `{flag}/domain/flag/Flag.java` — `participate()` 메서드 조건 수정

## Background
`capacity == null`은 정원 무제한을 의미하도록 `validateCapacity()`에서 설계되어 있다.
그러나 `participate()` 내부 조건이 `capacity == null || currentCount >= capacity`로 되어 있어,
null이면 무조건 `FlagFullCapacityException`을 던지는 논리 오류가 있다.

## Requirements
1. `Flag.participate()`의 정원 초과 조건을 `capacity != null && currentCount >= capacity`로 수정한다.
2. `capacity == null`이면 정원 제한 없이 참여할 수 있어야 한다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| null 의미 | 정원 무제한 | 기존 `validateCapacity()` 설계 그대로 유지 |
| 수정 범위 | `participate()` 조건 한 줄 | 다른 로직 변경 없음 |
