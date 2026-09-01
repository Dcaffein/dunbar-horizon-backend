# Task-102: flag validation 응답 정리

> **완료·main 병합** (`ai/fix-flag-validation-response`, 머지 `45d6eec`).
> **Domain Change:** [x] — 길이 상한을 도메인에 새긴다.
> **FE 협조:** [ ] — 응답 스키마 불변. 제목 초과가 500 → 400 + `validation.title`, 영문 문구가 한국어로 바뀐다.

## 왜

FE 문의 2건. (1) flag DTO들의 제약 어노테이션에 `message`가 없어 영문 기본 문구가 나간다. (2) 제목 256자가 DB `varchar(255)`에 닿아 `DataIntegrityViolationException` → 500으로 응답된다 — 입력 실수를 서버 장애로 오분류해 FE가 필드에 못 붙이고 재시도 안내만 띄운다.

길이 상한이 도메인·DTO·컬럼 어디에도 없다. 검증 소유 규칙([[feedback-validation-ownership]])을 적용한다: 길이는 제품 판단이므로 도메인이 원천, DTO가 상수를 참조.

## 상한 (결정됨)

- **제목**(`Flag.title`) — **20자**. 제목은 간결함을 강제하는 게 미덕.
- **설명**(`Flag.description`) — **500자**. 운영 실사용 최대 49·평균 19.6자의 약 10배 여유. (컬럼은 mediumtext라 500 에러가 아니라 "상한 부재"가 문제였다.)

## 알려진 제약

- 초과·blank는 신규 `FlagInvalidBasicInfoException`(400)으로 거절한다. 기존 blank는 `FlagInvalidStatusException`(409)이라 Comment·Memorial의 400과 어긋나 있었다.
- 문구 두 층: 전역 `ValidationMessages.properties`(기본 문구 한국어 override, 전 도메인 공통) + 필드별 인라인.
- Comment(500)·Memorial(1000)의 흩어진 리터럴을 상수로 모으고, Memorial 문구의 내부 타입명("flag memorial")을 사용자 문구로 교체.
- notification `DeviceTokenRequest.token`의 `@NotBlank` 누락도 전역 properties와 함께 rider로 처리.

## Out of Scope

- **상한 없는 컬럼 전반의 500 방지**(`DataIntegrityViolationException` 핸들러) — task-100. 핸들러는 어느 컬럼이 넘쳤는지 모르고, 필드 부착은 DTO `@Size`만이 만든다.
- `FlagScheduleUpdateRequest`(제약 어노테이션 없음), `GlobalExceptionHandler` 전반, message 문구 전 도메인 통일 — 별건.

## 관련

task-100(예외 처리 정비), [[feedback-validation-ownership]].
