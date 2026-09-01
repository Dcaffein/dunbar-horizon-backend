# PLAN — flag validation 응답 정리 (task-102)

## [도메인 수정 승인 요청]

`Domain Change: [x]`. title·description 길이 상한을 **제품 판단**으로 보고 도메인에 새긴다(로직 변경 아님, 상수·검증 메서드 추가).

- `Flag`에 `TITLE_MAX_LENGTH=20`, `DESCRIPTION_MAX_LENGTH=500` + 문구 상수를 올리고, title/description 검증(null·blank·초과)을 도메인이 소유한다.
- 검증 실패는 **400**으로 거절한다(신규 `FlagInvalidBasicInfoException`). 기존 blank는 `FlagInvalidStatusException`(409)이었으나 정상 경로에선 DTO가 먼저 거르므로 관측되는 계약 변화는 없다.
- `FlagMemorial` 문구의 내부 타입명("flag memorial")을 사용자 문구로 교체한다. Comment(500)·Memorial(1000)의 흩어진 리터럴을 상수로 모아 DTO가 참조하게 한다.

## 작업 목표

flag 입력 규격 검증을 완성해 제목 초과가 500이 아니라 400 + 필드 오류로 응답되게 하고, 영문 기본 문구를 한국어로 바꾼다. 길이 상한의 **원천은 도메인**, DTO `@Size`는 그 상수를 참조한다.

## 현황 분석

- `Flag.title`(`Flag.java:42`)에 `@Size`·`@Column(length)` 없음, 도메인도 길이 미검사 → 256자가 DB `varchar(255)`에서 `DataIntegrityViolationException` → catch-all → **500**. (title·description 모두 DTO엔 `@NotBlank`만 있어 blank는 이미 400: `FlagCreateRequest:11-12`, `FlagDetailsUpdateRequest:6-7`.)
- `description`은 `@Column(columnDefinition = "TEXT")`(`Flag.java:44`)라 길이 폭발은 사실상 도달 불가(운영 실사용 최대 49·평균 19.6자). 문제는 500이 아니라 **상한이 어디에도 없다**는 것.
- 도메인 blank 검증은 `FlagInvalidStatusException`(**409**, `Flag.java:225`)을 던져 Comment·Memorial의 400과 어긋난다. `updateBasicInfo`(`Flag.java:144`)에 blank 검사 중복.
- flag DTO 9개의 제약 어노테이션에 `message` 미지정 → 영문 기본 문구.
- `FlagMemorial` 문구(`FlagMemorial.java:37`)에 내부 타입명 노출. `FlagComment`는 `500`을 세 곳(`:30,:87,:88`)에 흩어 가짐.
- 이 프로젝트에 `LocalValidatorFactoryBean`·`MessageSource` 커스터마이징 없음 → `ValidationMessages.properties` 번들이 바로 먹는다.

## 변경 파일 목록

| 파일 | 변경 |
|---|---|
| `flag/domain/flag/Flag.java` | `TITLE_MAX_LENGTH=20`/`TITLE_LENGTH_MESSAGE`, `DESCRIPTION_MAX_LENGTH=500`/`DESCRIPTION_LENGTH_MESSAGE` 상수. title/description 검증 메서드 추출(null·blank·초과 → 400) 후 생성자·`updateBasicInfo` 공유, 중복 blank 제거. hostId null 검사는 유지. |
| `flag/domain/flag/exception/FlagInvalidBasicInfoException.java` (신규) | `extends FlagException`, `HttpStatus.BAD_REQUEST`. title·description 규격 위반. |
| `flag/domain/comment/FlagComment.java` | `CONTENT_MAX_LENGTH=500`/`CONTENT_LENGTH_MESSAGE` 상수로 리터럴 3곳 통합. |
| `flag/domain/memorial/FlagMemorial.java` | `CONTENT_MAX_LENGTH=1000`/`CONTENT_LENGTH_MESSAGE` 상수화 + 문구의 "flag memorial" 제거. |
| `src/main/resources/ValidationMessages.properties` (신규) | Bean Validation 기본 문구를 한국어로 덮음. **전 도메인 공통** — 이 파일 하나로 영문 재발을 막는다. |
| flag DTO 9개(+update 4) | 필드별 `@NotBlank`/`@Size`/`@Min` + `message`. title/description은 `@Size(max = Flag.*_MAX_LENGTH, message = Flag.*_LENGTH_MESSAGE)`로 **도메인 상수 참조**. comment/memorial content도 각 상수 참조. |
| `notification/adapter/in/web/dto/DeviceTokenRequest.java` | `token`에 `@NotBlank` 추가(현재 무제약). 기술 토큰이라 길이 상한은 없음 — 전역 properties와 함께 태우는 한 줄짜리 rider. |
| 테스트 | `FlagTest`(도메인 400 경계), `FlagControllerTest`/`FlagCommentControllerTest`/`FlagMemorialControllerTest`(응답 코드·문구·필드 귀속). |

DTO 목록: `FlagCreateRequest`, `FlagDetailsUpdateRequest`, `FlagCapacityUpdateRequest`, `CommentCreateRequest`, `CommentUpdateRequest`, `MemorialCreateRequest`, `MemorialUpdateRequest`, `FlagInviteRequest`, `FlagInvitePermissionRequest`. (`FlagScheduleUpdateRequest`는 제약 어노테이션이 없어 범위 밖.)

## 구현 방향

- **길이 상한의 원천은 도메인**(제품 판단). DTO `@Size`는 리터럴이 아니라 `public static final` 상수를 참조 → 도메인 한 곳만 고치면 DTO가 재컴파일로 따라온다(값 드리프트 소멸). [[feedback-validation-ownership]]
- 도메인은 모든 호출자에 대한 보증(백스톱), DTO는 경계 fast-fail(필드 귀속·모아서·한국어 400). 둘은 역할이 다르며 중복이 아니다.
- **문구는 두 층.** `ValidationMessages.properties`(기본 문구 override) + 필드별 인라인. `.properties`는 Java 21 UTF-8 직접 기재.
- **`DataIntegrityViolationException` 핸들러와 400/409 분기는 task-100으로 넘긴다.** 필드 부착은 DTO `@Size`만이 만든다.

## 예상 사이드 이펙트

- `POST /api/v1/flags`, `PATCH /api/v1/flags/{flagId}/details` 제목 21자 → **500 → 400 + `validation.title`**. 20자 → 성공(경계 확인).
- `message` 미지정 필드(전 도메인 + `trace/VisitRequestDto`)가 영문 → 한국어.
- `validation` 스키마·API URL·DTO 구조·상태 전이 규칙 불변.

## 커밋 분할

```
fix(flag): 제목·설명 길이 상한을 도메인에 새기고 400으로 거절한다
fix(global): validation 기본 문구를 한국어로 덮고 device token 필수를 지정한다
fix(flag): flag DTO에 필드별 validation 문구와 상한을 지정한다
```

각 커밋은 자체 테스트를 포함한다.

## 테스트 전략

건드린 flag 범위만 실행한다.

```bash
./gradlew test --tests '*FlagTest' --tests '*FlagControllerTest' --tests '*FlagCommentControllerTest' --tests '*FlagMemorialControllerTest'
```

- `FlagTest` — title 21자·blank가 `FlagInvalidBasicInfoException`(400), description 501자도 동일
- 컨트롤러 — 21자 → 400 + `validation.title`, 20자 → 성공 (경계 한 칸 밀림 방지)
- `message` 미지정 필드가 한국어로 나오는지(기본 문구 override 확인) 최소 1케이스
- 컨트롤러 테스트는 Testcontainers(Docker) 필요.
