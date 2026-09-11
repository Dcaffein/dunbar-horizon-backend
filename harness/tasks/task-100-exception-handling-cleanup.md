# Task-100: 예외 처리 정비

> 상태: 완료 (2026-09-11, main 머지). 배포 전이며 FE 대응이 선행되어야 한다.

> **Domain Change:** [x] — 도메인 예외 클래스의 생성자 시그니처를 바꾼다.
> 필드·비즈니스 로직은 건드리지 않는다.

> **FE 협조 필요:** [x] — `error` 값이 클래스명에서 코드로 바뀌고,
> 일부 500이 400/415로 바뀐다. 필드 구성과 `message`·`validation`·상태 코드는 그대로다.

## Background

### 예외가 응답이 되는 출구는 두 개다

`@RestControllerAdvice`는 **요청 스레드에서 컨트롤러를 거쳐 나온 예외**만 잡는다.
`DispatcherServlet` 밖이거나, 다른 스레드거나, 응답이 커밋된 뒤면 닿지 않는다.

| 경로 | 처리 주체 |
|---|---|
| 컨트롤러에서 나온 예외 | `GlobalExceptionHandler` (핸들러 9개) |
| 시큐리티 필터 체인 (인증 실패) | `JwtAuthenticationEntryPoint` |
| 시큐리티 필터 체인 (인가 실패) | **없음** — 3절 참조 |

**`ErrorResponse`를 만드는 곳이 두 군데라는 것이 이 task 전체에 걸린 함정이다.**
응답 형식을 바꿀 때 `JwtAuthenticationEntryPoint`를 빠뜨리면
인증 에러만 옛 형식으로 남는다.

### 지켜지고 있는 것 — 건드리지 말 것

`@ExceptionHandler(Exception.class)` catch-all이 예상 못 한 예외를 전부
500 + 고정 문구로 뭉개고 원본은 `log.error`로만 남긴다.
스택트레이스·SQL·드라이버 메시지가 응답으로 새지 않는다. **이 구조는 옳다.**

그로부터 나오는 원칙:

> **`e.getMessage()`를 응답에 넣어도 되는 것은 `BusinessException`뿐이다.**
> 그건 우리가 쓴 문장이기 때문이다. 그 외 예외는 전부 우리가 새로 쓴 문구를 넣는다.

현재 9개 핸들러가 이 선을 지킨다. **핸들러를 추가할 때도 지킨다.**

---

## 1. 에러 계약을 클래스명에서 분리한다 — 이 task의 본체

### 문제

`handleBusinessException`이 응답의 `error` 값을 클래스명에서 파생시킨다.

```java
.error(e.getClass().getSimpleName())   // "FlagFullCapacityException"
```

**클래스 이름을 바꾸면 API 응답이 바뀌는데, 어떤 도구도 잡아주지 못한다.**

- IDE Rename이 참조를 다 고쳐서 컴파일이 통과한다
- 테스트도 통과한다 — 예외 **타입**으로 검증하기 때문이다
- `"FlagFullCapacityException"`이라는 문자열은 **소스 어디에도 없다.**
  런타임에만 존재하므로 grep도 안 되고 PR diff에도 안 나타난다

즉 **계약을 깨는 행동이 단순 정리 작업처럼 보인다.**
flag 도메인은 방금 27커밋 규모로 리팩터링했으므로 가정이 아니다.

### 일관성이 반대로 되어 있다

9개 핸들러 중 8개는 `error`를 리터럴로 직접 쓴다.
`MethodArgumentNotValidException`을 잡아 `"InvalidInputException"`을 내보내는 식이다.

**이름이 바뀔 일 없는 스프링 예외는 보호되어 있고, 자주 바뀌는 도메인 예외만 무방비다.**

### 할 일

**(a) `BusinessException`에 코드를 추가한다**

```java
public abstract class BusinessException extends RuntimeException {
    private final String code;          // 추가
    private final HttpStatus httpStatus;
}
```

코드는 도메인별 enum으로 모은다(`FlagErrorCode`, `AccountErrorCode`, ...).
코드·메시지·status가 한곳에 모여 **에러 전체 목록을 파일 하나로 뽑을 수 있게** 된다.
지금은 그 목록이 어디에도 없다.

**(b) 네이밍은 `{DOMAIN}_{CONDITION}` SCREAMING_SNAKE_CASE**

```
FLAG_FULL_CAPACITY, FLAG_DEADLINE_PASSED, FLAG_NOT_FOUND
ACCOUNT_ALREADY_REGISTERED_EMAIL, ACCOUNT_INVALID_CREDENTIALS
```

**클래스명을 기계적으로 변환하지 말 것.** 코드는 클래스명과 독립적으로
살아남아야 하므로, 지금 클래스명이 어색하면 코드는 제대로 지어둔다.

**(c) 필드는 `error` + `message` 그대로 두고, `error`의 값을 코드로 바꾼다**

```json
{
  "error": "FLAG_FULL_CAPACITY",
  "message": "정원이 가득 찬 깃발입니다."
}
```

**초안은 `code` 필드를 신설하고 `error`를 남겼다가 FE 전환 후 별도 task로 지우는 안이었다.**
그 공존 기간의 존재 이유는 "BE만 먼저 배포되는 구간"인데, 이 릴리스에는 그 구간이 없다.
task-104/106/109로 account·flag·social의 URL이 전부 바뀌어 FE와 BE가 같이 나가야 하기 때문이다.
FE가 필드를 못 찾기 전에 엔드포인트를 못 찾는다.

판별자는 하나면 된다. 필드를 늘렸다 지우는 대신 `error`의 값만 바꾼다. `ErrorResponse`는
손댈 필요가 없고, FE도 값 비교만 고치면 된다.

**(d) 스프링 예외 8개의 `error` 리터럴도 코드로 옮긴다**

| 기존 `error` | 새 `code` |
|---|---|
| `InvalidInputException` | `INVALID_INPUT` |
| `InvalidJsonFormatException` | `INVALID_JSON_FORMAT` |
| `AccessDeniedException` | `ACCESS_DENIED` |
| `NotFoundException` | `RESOURCE_NOT_FOUND` |
| `MethodNotAllowedException` | `METHOD_NOT_ALLOWED` |
| `InvalidRequestException` | `INVALID_REQUEST` |
| `ConcurrentModificationException` | `CONCURRENT_MODIFICATION` |
| `InternalServerException` | `INTERNAL_SERVER_ERROR` |

`ConcurrentModificationException`은 `java.util`에 **같은 이름의 실제 예외가 있고
의미가 전혀 다르다.** 코드로 옮기면서 자연히 해소된다.

**(e) `JwtAuthenticationEntryPoint`를 동일하게 맞춘다**

`BusinessException`이면 `code`를 꺼내 쓰고, 아니면 `UNAUTHORIZED`를 기본값으로.

### 규모

예외 클래스 65개. base 11개와 구체 54개다
(flag 24, social 21, account 9, buzz 4, notification 2, trace 2, global 3).
**flag부터 시작한다.** 가장 많고, 방금 리팩터링해서 이 문제를 실제로 겪을 자리다.

> 초안 작성 시점(2026-08-21)에는 62개였다. task-102·111·112 검증 롤아웃으로
> `FlagInvalidBasicInfoException`, `FlagInvitationInvalidException`,
> `InvalidFriendAliasException` 셋이 늘었다. 셋 다 도메인이 문구를 넘기는
> `(String message)` 생성자라, 코드 enum이 메시지를 소유할 수 없다는 근거가 된다.
> **enum은 `code`와 `status`만 갖고 메시지는 예외 클래스에 남긴다.**

### 함정 — 사용자 문구와 로그 문구가 섞여 있다

```java
super("존재하지 않는 flag : " + flagId, HttpStatus.NOT_FOUND);
```

`"존재하지 않는 flag : 331"`은 로그 문구지 사용자에게 보여줄 문장이 아니다.
분리하는 게 맞으나 범위가 커지므로 **이 task에서는 발견되는 것만 기록하고 넘어간다.**

---

## 2. catch-all로 떨어지는 예외 두 개를 덮는다

`GlobalExceptionHandler`는 `ResponseEntityExceptionHandler`를 상속하지 않아
스프링 기본 웹 예외 처리도 못 받는다. 선언한 9개 외에는 전부 500이 나간다.

catch-all 자체는 안전장치로 옳게 동작한다. 문제는 **400이어야 할 것이 500으로 나가는 것**이다.
사용자는 재시도해도 소용없는데 "잠시 후 다시 시도해주세요"를 보고,
FE는 이걸 서버 장애로 취급해 에러 리포팅까지 쏜다.

**(a) `MethodArgumentTypeMismatchException` → 400**

`/api/v1/flags/abc`처럼 `Long` 경로변수에 숫자가 아닌 값이 올 때 발생한다.
`TypeMismatchException` 계열이라 기존 `ServletRequestBindingException` 핸들러가
잡지 못한다(상속 관계가 없다). **실제로 발생 가능한 경로다.**

응답 메시지에 **사용자가 보낸 값을 되비추지 말 것.** 파라미터 이름만 쓴다.

**(b) `HttpMediaTypeNotSupportedException` → 415**

**(c) 로그 누락 두 곳** — `NoResourceFoundException`(404)과
`HttpRequestMethodNotSupportedException`(405) 핸들러에는 로그가 없다.
나머지 7개는 warn/error를 남긴다. `log.warn`을 추가한다.

> `jakarta.validation.ConstraintViolationException`은 `@Validated`를 쓰는 곳이
> 현재 없어 발생하지 않는다. `@Validated` 도입 시 함께 추가한다.

---

## 3. 인가(403) 출구를 만든다

`SecurityConfig.exceptionHandling`에 `authenticationEntryPoint`만 있고
`accessDeniedHandler`가 없다.

**지금은 발동 경로가 없어 무해하다** — 인가 규칙이 `.anyRequest().authenticated()`뿐이고,
`@EnableMethodSecurity`도 `@PreAuthorize`도 없으며, `new AccessDeniedException(...)`을
직접 던지는 코드도 없다. (그래서 `GlobalExceptionHandler`의 `AccessDeniedException`
핸들러는 현재 사실상 죽어 있다.)

문제는 **`ROLE_ADMIN` 규칙을 추가하는 순간**이다. 필터 체인의 403은 advice가 아니라
시큐리티 기본 핸들러로 가고, 그건 `ErrorResponse` 형식이 아닌 컨테이너 기본 응답을 낸다.
**인증(401)은 출구를 맞춰놨는데 인가(403)는 안 맞춰놓은 상태다.**

`JwtAuthenticationEntryPoint`와 같은 방식으로 `ErrorResponse`를 쓰는
`AccessDeniedHandler`를 만들어 등록한다.

---

## 커밋 분할

절 순서대로 나눈다. 1절이 가장 크므로 도메인 단위로 더 쪼갠다.

```
feat(global): ErrorCode를 도입하고 BusinessException을 코드 기반으로 바꾼다
refactor(flag): flag 예외 24개에 error code를 부여한다
refactor(social): social 예외 21개에 error code를 부여한다
refactor(account): account 예외 9개에 error code를 부여한다
refactor(global): 나머지 도메인 예외에 error code를 부여한다
fix(global): 두 출구의 error 값을 클래스명 파생에서 코드로 바꾼다
fix(global): 타입 불일치·미지원 미디어타입을 4xx로 처리한다
fix(global): 인가 실패 응답 형식을 통일한다
```

**주의.** `BusinessException`의 생성자를 교체하면 65개가 한꺼번에 깨진다. 커밋마다 빌드가
서게 하려면 첫 커밋에서 새 생성자를 추가만 하고, 마지막 사용처가 사라지는 다섯째 커밋에서
옛 생성자를 지워야 한다.

## Out of Scope

- **RFC 9457(Problem Details) 전환.** 스프링 부트 3.4는 `ProblemDetail`을 지원하지만,
  소비자가 자사 FE 하나뿐이라 표준 준수의 실익이 전환 비용을 넘지 않는다.
  `validation: {필드: 메시지}`도 현재 형태가 더 쓰기 좋다.
  **표준의 핵심 교훈(안정적 식별자를 명시적으로 둔다)은 1절로 이미 취한다.**
- **`ResponseEntityExceptionHandler` 상속 전환.** 응답 형식이 `ProblemDetail`로 바뀌어
  FE 전면 수정이 필요하다. 개별 핸들러 추가가 싸다.
- **도메인 예외에서 `HttpStatus` 분리.** 도메인이 HTTP를 아는 것은 헥사고날 위반이지만,
  REST 하나만 노출하는 현재 실익이 없다. gRPC·배치로 재사용하게 되면 그때.

  > 2026-09-11 구현 중 재검토했고 같은 결론을 유지했다. 코드가 `{Domain}ErrorCode` enum으로
  > 옮겨가면서 status도 함께 도메인에 남았다.
  >
  > 걷어내려면 웹 어댑터가 코드별로 status를 정해야 하는데, 공통 핸들러는 `ErrorCode`
  > 인터페이스만 받으므로 도메인별 매퍼를 빈으로 두고 주입받는 배선이 필요하다. 간접이 한 겹
  > 늘고 에러 하나를 따라가는 데 예외 → enum → 매퍼 세 단계를 거친다.
  >
  > 중간에 `ErrorKind`(NOT_FOUND·CONFLICT·GONE 같은 성격 분류)를 두는 안도 검토했으나
  > **폐기했다.** 이름이 전부 HTTP 상태에서 역산한 것이라, 도메인에서 HTTP를 걷어낸 것처럼
  > 보이게 할 뿐 실제로는 이름만 바꾼 것이다. `GONE`이 그 증거다.
  >
  > 지금 status가 enum 한 곳에 모여 있어 분리 시점이 오면 값만 떼면 된다. 예외 54개에
  > 흩어져 있던 이전보다 나은 위치다. **재검토 트리거는 두 번째 노출 어댑터다.**
- `message` 문구 자체의 정리
- **`@Async`·`@Scheduled` 예외 처리.** 검토했으나 할 일이 없다고 판단했다.
  `SimpleAsyncUncaughtExceptionHandler`가 메서드명을 포함해 ERROR 로그를 남기고,
  `TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER`가 스케줄을 죽이지 않고 로그를 남긴다.
  남는 격차는 **스케줄러 로그가 8개 중 어느 것인지 메시지에 안 밝힌다**는 것 하나뿐이고
  (스택트레이스에는 있다), 절 하나를 차지할 무게가 아니다.
  스케줄러 실패 알림이 실제로 필요해지면 그때 별도로 다룬다.
- 재시도·dead-letter 설계 — `TraceService`의 `@Retryable`, outbox 재시도 스케줄러 등
  필요한 곳에는 이미 있다

## 검증 방법

- **기존 예외 테스트는 타입으로 검증하므로 그대로 통과한다 — 통과가 곧 검증이 아니다.**
  `code` 값을 문자열로 단언하는 테스트를 추가해야 클래스명을 바꿔도 계약이 지켜지는지 확인된다
- 인증 실패(401) 응답에 `code`가 실리는지 — **출구가 둘이라 놓치기 쉽다**
- `/api/v1/flags/abc`가 400을 내는지 (지금은 500), 잘못된 Content-Type이 415를 내는지
- 응답 본문에 스프링 예외의 원본 메시지가 섞이지 않는지 눈으로 확인
- `accessDeniedHandler`는 발동 경로가 없어 통합 테스트가 어렵다.
  임시로 역할 기반 규칙을 걸어 확인 후 되돌리거나, 최소한 등록만 하고 넘어간다
- 전체 코드 목록을 뽑아 FE에 전달한다
