# Task-56: 내 프로필 조회 및 소셜 유저 프로필 조회 API 추가

## Background

프론트엔드에서 두 가지 유저 조회 기능이 필요했다.

1. 로그인한 유저가 자신의 개인정보(email 포함)를 조회하는 기능
2. 다른 유저의 프로필 카드를 조회하는 경량 기능

기존 `UserController`에는 이메일 검색(`GET /api/v1/users/search`)만 존재했고,
`UserProfileInfo`에 email이 포함되지 않아 본인 정보 조회 DTO로 재사용할 수 없었다.

소셜 프로필은 Account(MySQL)를 거치지 않고 Social(Neo4j)의 `SocialUser`를 직접 반환하는 것이
적합하다고 판단했다. `SocialUser`는 id, nickname, profileImageUrl을 보유하며
소셜 프로필 카드 수준에서 필요한 정보를 충족한다.
Neo4j miss 시 `SocialUserSyncHelper`가 MySQL에서 자동 동기화하므로 정합성도 보장된다.

## Objective

- `GET /api/v1/users/me` — 로그인 유저 본인 전체 프로필 조회 (email 포함)
- `GET /api/v1/social/users/{id}` — 타인의 경량 소셜 프로필 조회

## Decision

| 엔드포인트 | 도메인 | 데이터 소스 | 반환 필드 |
|-----------|--------|-----------|---------|
| `GET /api/v1/users/me` | Account | MySQL | id, email, nickname, profileImageUrl |
| `GET /api/v1/social/users/{id}` | Social | Neo4j | id, nickname, profileImageUrl |

`UserProfileInfo`는 다른 도메인(Flag 등)에서 email 없이 사용 중이므로 건드리지 않고
`MyProfileResult` DTO를 별도 신설했다.

소셜 프로필은 `SocialUserQueryUseCase` 포트를 신설하고 `SocialUserService`가 구현하도록 했다.
도메인 객체(`UserReference`)를 컨트롤러 응답으로 직접 노출하지 않기 위해
`SocialProfileResult` DTO를 추가했다.

## Result

- `MyProfileResult` (신규 DTO), `SocialProfileResult` (신규 DTO)
- `SocialUserQueryUseCase` (신규 포트)
- `UserQueryUseCase.getMyProfile()` 메서드 추가
- `SocialUserService` — `SocialUserQueryUseCase` implements 추가
- `UserController` — `GET /api/v1/users/me` 추가
- `SocialUserController` (신규) — `GET /api/v1/social/users/{id}`
- 컨트롤러 테스트 2개, 서비스 테스트 2개 추가
- 브랜치 `ai/feat-user-profile-query` → main 머지 완료
