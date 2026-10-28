# Task: @CurrentUserId Swagger 파라미터 노출 제거

## Background

`@CurrentUserId`는 JWT 쿠키에서 사용자 ID를 추출하는 커스텀 파라미터 어노테이션으로,
`CurrentUserIdArgumentResolver`가 Spring MVC 레벨에서 처리한다.

그러나 springdoc-openapi는 이 커스텀 resolver를 인식하지 못해,
모든 컨트롤러 메서드의 `@CurrentUserId Long currentUserId`를 일반 요청 파라미터로 렌더링한다.

결과적으로 프론트엔드가 orval로 생성한 API 클라이언트에 `currentUserId`가 명시적 파라미터로
포함되어 잘못된 API 스펙이 생성된다.

## Objective

- `@CurrentUserId`가 붙은 파라미터를 Swagger 스펙에서 숨긴다.
- 모든 컨트롤러에 일괄 적용되어야 하며, 컨트롤러별 수동 처리는 하지 않는다.

## Domain Change

[ ] 없음  [x] 있음
- `global/annotation/CurrentUserId.java` — 메타 어노테이션 추가

## Decision

`@CurrentUserId`에 `@Parameter(hidden = true)` 메타 어노테이션을 추가한다.
springdoc-openapi는 `@Parameter(hidden = true)`가 붙은 파라미터를 스펙에서 자동 제외한다.

```java
// 현재
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {}

// 개선
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)   // io.swagger.v3.oas.annotations.Parameter
public @interface CurrentUserId {}
```

변경 파일: `global/annotation/CurrentUserId.java` 1개.
컨트롤러 수정 불필요.

## Result

- 브랜치: `ai/fix-swagger-hide-current-user-id`
- 커밋: `fdae12c`
- 변경 파일: `global/annotation/CurrentUserId.java` — `@Parameter(hidden = true)` 및 import 추가
