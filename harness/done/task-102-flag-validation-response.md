# Task-102: flag validation 응답 정리

> **완료·main 병합** (`ai/fix-flag-validation-response`, 머지 `45d6eec`).
> **Domain Change:** [x] — 길이 상한을 도메인에 새긴다.
> **FE 협조:** [ ] — 응답 스키마 불변. 제목 초과가 500 → 400 + `validation.title`, 영문 문구가 한국어로 바뀐다.

## 왜

FE 문의 2건. (1) flag DTO들의 제약 어노테이션에 `message`가 없어 영문 기본 문구가 나간다. (2) 제목 256자가 DB `varchar(255)`에 닿아 500으로 응답된다 — 입력 실수를 서버 장애로 오분류해 FE가 필드에 못 붙이고 재시도 안내만 띄운다.

길이 상한이 도메인·DTO·컬럼 어디에도 없다. 검증 소유 규칙([[feedback-validation-ownership]]) 적용: 길이는 제품 판단이므로 도메인이 원천.

## 상한 (결정됨)

- **제목**(`Flag.title`) — **20자**.
- **설명**(`Flag.description`) — **500자**. (컬럼은 mediumtext라 500 에러가 아니라 "상한 부재"가 문제였다.)

## Out of Scope

- **상한 없는 컬럼 전반의 500 방지**(`DataIntegrityViolationException` 처리) — task-100. 필드 부착은 DTO만 만들 수 있고 핸들러는 어느 컬럼이 넘쳤는지 모른다.
- `FlagScheduleUpdateRequest`(제약 어노테이션 없음), `GlobalExceptionHandler` 전반, message 문구 전 도메인 통일.

## 관련

task-100(예외 처리 정비), [[feedback-validation-ownership]].
