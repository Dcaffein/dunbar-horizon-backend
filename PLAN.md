# PLAN: 내 프로필 조회 및 소셜 프로필 조회 API 추가

## 목표

- `GET /api/v1/users/me` — 로그인한 유저 본인의 전체 프로필 조회 (Account)
- `GET /api/v1/social/users/{id}` — 타인의 경량 소셜 프로필 조회 (Social)

---

## 변경 파일 목록

### Account — `GET /api/v1/users/me`
1. `account/application/dto/MyProfileResult.java` (신규)
2. `account/application/port/in/UserQueryUseCase.java` (메서드 추가)
3. `account/application/service/UserQueryService.java` (구현 추가)
4. `account/adapter/in/web/UserController.java` (엔드포인트 추가)

### Social — `GET /api/v1/social/users/{id}`
5. `social/application/dto/result/SocialProfileResult.java` (신규)
6. `social/application/port/in/SocialUserQueryUseCase.java` (신규 포트)
7. `social/application/service/SocialUserService.java` (포트 구현 추가)
8. `social/adapter/in/web/SocialUserController.java` (신규 컨트롤러 — `/api/v1/social`)

## 브랜치
`ai/feat-user-profile-query` (from main)
