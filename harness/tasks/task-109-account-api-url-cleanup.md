# Task-109: Account API URL 정리

## Objective

자체 공개 REST API의 버전 규칙을 `/api/v1`으로 통일한다.

| 기존 | 목표 |
|---|---|
| `POST /api/auth/users` | `POST /api/v1/auth/users` |
| `POST /api/auth/tokens` | `POST /api/v1/auth/tokens` |
| `DELETE /api/auth/tokens` | `DELETE /api/v1/auth/tokens` |
| `PATCH /api/auth/tokens` | `PATCH /api/v1/auth/tokens` |
| `POST /api/auth/verifications` | `POST /api/v1/auth/verifications` |
| `GET /api/auth/verifications/{token}` | `GET /api/v1/auth/verifications/{token}` |
| `PATCH /api/auth/users/me` | `PATCH /api/v1/users/me` |
| `POST /api/auth/users/me/profile-image/presign` | `POST /api/v1/users/me/profile-image/presign` |

## Decision

- 기존 `/api/auth/**` controller 별칭은 유지하지 않는다. 클라이언트는 목표 URL로 전환한다.
- OAuth/Spring Security 경로(`/oauth2/**`, `/login/oauth2/**`), 루트 health check,
  `/api/dev/**`는 변경하지 않는다.
- 가입·로그인·토큰 갱신·로그아웃·검증 토큰 조회의 공개 보안 정책은 URL 이관 후에도 동일하게
  유지한다. 사용자 리소스 두 개는 계속 인증이 필요하다.

## Out of Scope

- 프론트엔드 변경
- OAuth redirect/callback, health check, dev/perf API 변경
- JWT cookie 정책, 도메인 규칙, DB 스키마 변경

## Resolution

- `AccountController`는 `/api/v1/auth` 아래의 인증·검증 API만 제공하도록 정리했다.
- 프로필 수정과 profile-image presign은 `UserController`로 옮겨 `/api/v1/users/me/**`로
  제공한다.
- `SecurityConfig`의 공개 matcher, JWT filter 주석, controller 테스트와 `CLAUDE.md`의 API
  문서를 새 계약으로 갱신했다.
- `AccountControllerTest` 13개, `UserControllerTest` 7개,
  `JwtAuthenticationFilterTest` 4개, `SocialUserControllerTest` 2개가 통과했다.
