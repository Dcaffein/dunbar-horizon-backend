# Task 37: 이메일로 유저 검색 API 추가

## Objective
콜드 스타트 문제 해소를 위해 이메일로 활성 유저를 조회하는 API를 추가한다.
검색 → social 프로필 진입 → 친구 요청 흐름으로 이어지게 한다.

## Domain Change
[x] 있음  [ ] 없음
- account 도메인에만 변경. social 도메인은 건드리지 않는다.

## Decision
- 엔드포인트: `GET /api/v1/users/search?email=` (인증 필요, 신규 `UserController`)
- `ACTIVE` 상태 유저만 반환. 응답에 이메일 미포함 (`id`, `nickname`, `profileImage`).
- 이메일 미등록 시 404 반환.
- Neo4j 노드 없는 엣지케이스는 social 프로필 진입 시 기존 Lazy Sync가 처리하므로 별도 대응 없음.

---

## Result

**브랜치:** `ai/feat-user-search-by-email`
**커밋:** `531efc4` (구현), `bda38f1` (테스트)

### 변경 내용

#### 구현
- `UserQueryUseCase` — `findActiveUserByEmail(String email)` 추가
- `UserQueryService` — `UserRepository.findByEmail()` + `ACTIVE` 상태 필터로 구현
- `UserController` (신규) — `GET /api/v1/users/search?email=`, 미등록 시 `UserNotFoundException(404)`

#### 테스트
- `UserQueryServiceTest` — ACTIVE 유저 반환 / PENDING 유저 필터 / 미등록 이메일 빈 Optional 검증 (3케이스)
- `UserControllerTest` — 200 OK(유저 존재) / 404(미등록) 검증 (2케이스)
- `BaseControllerTest` — `UserQueryUseCase` MockitoBean 추가

### 테스트 결과
5/5 PASSED
