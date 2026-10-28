# Task-100: 예외 처리 정비

> 상태: 구현 완료. 배포 전이며 FE 대응이 선행되어야 한다.

> **Domain Change:** [x] — 도메인 예외의 생성자와 에러 코드 enum을 바꾼다.
> 필드·비즈니스 로직·예외 발생 조건은 건드리지 않는다.

> **FE 협조 필요:** [x] — `error` 값이 바뀌고 코드 수가 늘어난다. 일부 status가 교정되고
> 일부 `message` 문구가 바뀐다. 필드 구성과 `validation`은 그대로다.

## 작업 전 상황

### 예외가 응답이 되는 출구는 셋이다

`@RestControllerAdvice`는 **요청 스레드에서 컨트롤러를 거쳐 나온 예외**만 잡는다.
`DispatcherServlet` 밖이거나, 다른 스레드거나, 응답이 커밋된 뒤면 닿지 않는다.

| 경로 | 처리 주체 |
|---|---|
| 컨트롤러에서 나온 예외 | `GlobalExceptionHandler` |
| 시큐리티 필터 체인 (인증 실패) | `JwtAuthenticationEntryPoint` |
| 시큐리티 필터 체인 (인가 실패) | `JwtAccessDeniedHandler` |

**`ErrorResponse`를 만드는 곳이 여럿이라는 것이 이 task 전체에 걸린 함정이다.** 응답 형식을
바꿀 때 하나를 빠뜨리면 그 경로만 옛 형식으로 남는다. 세 번째 출구는 이 작업에서 만들었다.

### 다섯 가지가 어긋나 있었다

**하나. 판별자가 클래스명에서 파생됐다.**

```java
.error(e.getClass().getSimpleName())   // "FlagFullCapacityException"
```

클래스 이름을 바꾸면 API 응답이 바뀌는데 어떤 도구도 잡아주지 못한다. IDE Rename은 참조를 다
고쳐 컴파일이 통과하고, 테스트는 예외 **타입**으로 검증하므로 통과하고, 그 문자열은 소스
어디에도 없어 grep도 diff도 안 된다. **계약을 깨는 행동이 단순 정리 작업처럼 보인다.**

**둘. 문구 하나가 응답과 로그를 겸했다.**

```java
super("존재하지 않는 flag : " + flagId, HttpStatus.NOT_FOUND);
```

`"존재하지 않는 flag : 331"`이 응답에 그대로 실렸다. 프론트는 이걸 띄울 수 없어 상태 코드별로
문구를 통째로 덮어쓰고 있었다.

**셋. 코드 하나가 조건 여럿을 포괄했다.** 20개 코드 뒤에 서로 다른 문장이 57개 있었다.
`FLAG_INVALID_STATUS` 하나에 10개, `FLAG_ACCESS_DENIED`에 9개가 매달려 있었다.

```
FLAG_INVALID_STATUS + "모임 시작 후에는 시간을 수정할 수 없습니다."
FLAG_INVALID_STATUS + "모집 중인 플래그에만 초대할 수 있습니다."
```

**무슨 일이 있었는지를 한국어 문장만 알았다.** 코드가 판별자 역할을 절반만 했다.

**넷. 400이어야 할 것이 500으로 나갔다.** `GlobalExceptionHandler`가
`ResponseEntityExceptionHandler`를 상속하지 않아 선언한 핸들러 외에는 전부 catch-all로 떨어졌다.

**다섯. 인가(403) 출구가 없었다.** 인증(401)은 `ErrorResponse`로 맞춰놨는데 인가는
컨테이너 기본 응답이 나갔다.

## 결정사항

### 판별자는 `error` 필드의 값이다. 필드를 늘리지 않는다

```json
{ "error": "FLAG_NOT_FOUND", "message": "요청하신 깃발을 찾을 수 없습니다." }
```

`code` 필드를 새로 만들고 `error`를 남겨두는 안을 검토했으나 판별자가 둘일 이유가 없다.
값만 바꾸면 `ErrorResponse`를 손댈 필요가 없고 FE도 값 비교만 고치면 된다.

### `status`는 도메인의 `ErrorCode`가 갖는다

도메인이 HTTP를 아는 것은 헥사고날 위반이다. 걷어내려면 웹 어댑터가 코드별로 status를 정해야
하는데, 공통 핸들러는 `ErrorCode` 인터페이스만 받으므로 도메인별 매퍼를 빈으로 두고 주입받는
배선이 필요하다. 간접이 한 겹 늘고 에러 하나를 따라가는 데 세 단계를 거친다.

**노출 어댑터가 REST 하나뿐인 현재는 그 값을 치를 이유가 없어 타협했다.** status가 enum 한
곳에 모여 있으므로 분리 시점이 오면 값만 떼면 된다. 예외 54개에 흩어져 있던 이전보다 나은
위치다. **재검토 트리거는 두 번째 노출 어댑터다.**

> 중간에 `ErrorKind`(NOT_FOUND·CONFLICT·GONE 같은 성격 분류)를 두는 안을 검토했으나
> 폐기했다. 이름이 전부 HTTP 상태에서 역산한 것이라 도메인에서 HTTP를 걷어낸 것처럼 보이게
> 할 뿐이다. `GONE`이 그 증거다.

### 코드는 조건 하나당 하나다

57개 문장을 조건 단위로 묶어 코드를 93개로 나눴다. 같은 사실을 다른 동작에서 말하는 문장은
한 코드로 합쳤다(만료된 Buzz에 댓글 작성·수정·삭제 → `BUZZ_EXPIRED`).

쪼개면서 status 분류 오류 셋이 드러났다. 굵은 코드에 몰아넣은 탓에 보이지 않던 것이다.

| 위치 | 조건 | 전 | 후 |
|---|---|---|---|
| `Buzz` | 댓글을 찾지 못함 | 400 | **404** |
| `Flag` | 호스트 정보 필수 | 409 | **400** |
| `FlagSchedule` | 일정 검증 3건 | 409 | **400** |

`AuthNotFoundException`은 호출부가 0건이라 삭제했다.

### `message`는 `code`의 설명이다

`ErrorCode`가 문구를 소유한다. enum 생성자가 문구를 요구하므로 누락이 컴파일 에러가 된다.

**문장에 값을 섞지 않는다.** 문장에 박힌 값은 클라이언트가 꺼내 쓸 수 없고 문구를 고치면
깨진다. 값은 예외의 컨텍스트로 로그에 남으며, 클라이언트가 필요로 하면 별도 필드로 연다.

이 문장을 화면에 그대로 쓸지 자체 문구로 대체할지는 클라이언트가 정한다.

> `messages.properties` + `MessageSource` 방식을 검토했으나 채택하지 않았다. enum은 누락이
> 컴파일 에러인데 properties는 키 누락을 테스트로만 막을 수 있다. 다국어 요구가 실제로 생기면
> 그때 옮기며, 이동은 기계적이다.

### 예외는 로그 컨텍스트만 든다

```
응답   { "error": "FLAG_FULL_CAPACITY", "message": "정원이 가득 찬 깃발입니다." }
로그   FLAG_FULL_CAPACITY {flagId=331, capacity=8, participantCount=8}
```

**로그에 문장을 싣지 않는다.** 코드가 이미 조건을 정확히 말하므로 문장은 같은 말의 반복이고,
한글 문장은 콘솔 코드페이지나 로그 수집기를 거치며 깨질 수 있다. 로그가 `코드 + 키=값`
형태라 기계 파싱도 된다.

조립은 예외 생성자에서 한다. 그래야 잡히지 않고 올라간 경우에도 스택트레이스에 남는다.

인자를 하나도 받지 않던 예외 11개에도 컨텍스트를 줬다. 그 전에는 터져도 로그에 값이 하나도
남지 않아 사용자 제보를 추적할 수 없었다.

담는 것의 원칙은 **재현과 조회에 필요한 식별자 + 판정에 쓰인 값**이다. 엔티티를 통째로 넣지
않는다. 비밀번호와 토큰 원문은 어떤 컨텍스트에도 넣지 않는다.

### 남은 4xx를 덮고 인가 출구를 만들었다

- `MethodArgumentTypeMismatchException` → 400. `/api/v1/flags/abc`처럼 `Long` 경로변수에
  숫자가 아닌 값이 올 때 발생한다. `TypeMismatchException` 계열이라 기존
  `ServletRequestBindingException` 핸들러가 잡지 못한다(상속 관계가 없다).
  응답에는 파라미터 이름만 싣는다. 사용자가 보낸 값을 되비추면 그대로 반사되는 통로가 된다.
- `HttpMediaTypeNotSupportedException` → 415.
- `JwtAccessDeniedHandler`를 만들어 `SecurityConfig`에 등록했다. 인가 규칙이
  `anyRequest().authenticated()`뿐이라 지금은 발동 경로가 없다. 규칙보다 출구를 먼저 만들어
  둔 것이며, `ROLE_ADMIN` 규칙을 추가하는 순간 형식이 어긋나는 것을 막는다.

### 지켜지고 있던 것 — 건드리지 않았다

`@ExceptionHandler(Exception.class)` catch-all이 예상 못 한 예외를 전부 500 + 고정 문구로
뭉개고 원본은 `log.error`로만 남긴다. 스택트레이스·SQL·드라이버 메시지가 응답으로 새지 않는다.

> **원본 메시지를 응답에 실어도 되는 것은 `BusinessException`뿐이다.** 그 외 예외는 전부
> 우리가 새로 쓴 문구를 넣는다. 핸들러를 추가할 때도 지킨다.

## 예외를 새로 만들 때

1. `{Domain}ErrorCode`에 **조건 하나당 코드 하나**를 추가한다. 코드에 status와 문구를 함께 적는다.
2. 예외 생성자는 **로그 컨텍스트만** 받는다. 로그 문장을 손으로 쓰지 않는다.
3. 같은 코드를 서로 다른 문장으로 던지고 싶어지면 **코드를 쪼갤 신호다.** 문장에만 들어 있는
   정보는 클라이언트가 분기에 쓸 수 없다.

## Out of Scope

- **RFC 9457(Problem Details) 전환.** 스프링 부트 3.4는 `ProblemDetail`을 지원하지만,
  소비자가 자사 FE 하나뿐이라 표준 준수의 실익이 전환 비용을 넘지 않는다.
  `validation: {필드: 메시지}`도 현재 형태가 더 쓰기 좋다.
  **표준의 핵심 교훈(안정적 식별자를 명시적으로 둔다)은 이미 취했다.**
- **`ResponseEntityExceptionHandler` 상속 전환.** 응답 형식이 `ProblemDetail`로 바뀌어
  FE 전면 수정이 필요하다. 개별 핸들러 추가가 싸다.
- **응답에 컨텍스트를 구조화된 필드로 싣는 것.** 프론트가 요청한 적이 없고, 열면 이메일이나
  내부 id까지 같이 나갈 위험이 생긴다. 필요해지면 코드별로 골라서 연다.
- **`@Async`·`@Scheduled` 예외 처리.** 검토했으나 할 일이 없다.
  `SimpleAsyncUncaughtExceptionHandler`가 메서드명을 포함해 ERROR 로그를 남기고,
  `TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER`가 스케줄을 죽이지 않고 로그를 남긴다.
- 재시도·dead-letter 설계 — `TraceService`의 `@Retryable`, outbox 재시도 스케줄러 등
  필요한 곳에는 이미 있다.

> `jakarta.validation.ConstraintViolationException`은 `@Validated`를 쓰는 곳이 현재 없어
> 발생하지 않는다. `@Validated` 도입 시 핸들러를 함께 추가한다.

## 검증 방법

- **기존 예외 테스트는 타입으로 검증하므로 그대로 통과한다 — 통과가 곧 검증이 아니다.**
  `error` 값을 문자열로 단언해야 클래스명을 바꿔도 계약이 지켜지는지 확인된다
- 응답 `message`에 ID가 없고, 예외의 `getMessage()`에는 남아 있다
- 로그 줄이 `코드 {키=값}` 형태이고 사용자 문장이 없다
- 쪼갠 코드가 조건과 1:1인지. 같은 코드가 두 조건에서 나오면 덜 쪼갠 것이다
- status 교정 3건이 실제로 404·400을 낸다
- 인증 실패(401) 응답에도 코드가 실리는지 — **출구가 셋이라 놓치기 쉽다**
- `/api/v1/flags/abc`가 400을, 잘못된 Content-Type이 415를 내는지
- 토큰 원문·비밀번호가 어떤 컨텍스트에도 들어가지 않는지
- `accessDeniedHandler`는 발동 경로가 없어 통합 테스트가 어렵다. 핸들러를 직접 호출해 검증한다
- 전체 코드 목록을 뽑아 FE에 전달한다
