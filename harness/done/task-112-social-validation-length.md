# Task-112: social 입력 길이 검증 원천 정리

> **Domain Change:** [x] — 길이 상수를 도메인에 올린다. 값·로직은 그대로다.
> **FE 협조:** [ ] — 상한 값(20)과 응답 스키마 모두 불변. 리팩터링에 가깝다.

## 왜

social은 buzz와 다르다 — 길이 제한이 **이미 있으나 잘못된 곳에 있다.** 라벨 이름·친구 별명의 상한 `20`이 **DTO에 리터럴로 박혀 있고**(`LabelCreateRequest`·`LabelUpdateRequest`에 복사, `FriendUpdateRequest`), 도메인엔 원천이 없다. 도메인(`Label`·`FriendRecognition`)은 길이를 보지 않는다.

검증 소유 규칙([[feedback-validation-ownership]]) 위반이다: 길이는 제품 판단이므로 도메인이 원천을 갖고 DTO가 참조해야 한다. Create/Update에 같은 리터럴이 복사돼 있는 것이 바로 그 규칙이 막으려는 값 드리프트다.

## 상한 (유지)

- 라벨 이름 **20자**, 친구 별명 **20자**. 이미 운영 중이라 값은 그대로 둔다. 이 task는 값이 아니라 **원천의 위치**를 고친다.

## Out of Scope

- 라벨 멤버 수 제한(150, DunbarCircle).
- 영문 기본 문구 override — task-102의 전역 `ValidationMessages.properties`가 이미 처리.
- enum·ID 파라미터 유효성.

## 관련

task-102(flag 선례), account 도메인(이미 `User.NICKNAME_*` 상수 참조로 모범), [[feedback-validation-ownership]].
