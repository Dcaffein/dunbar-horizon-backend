# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test
```

> 통합 테스트는 Docker가 필요하다(Testcontainers: MySQL, MongoDB).
> 컨테이너 이미지와 버전의 정본은 `src/test/java/com/example/DunbarHorizon/support/TestContainerConfig.java`다.

## 문서 지도 (Document Map)

작업 시작 전 아래 문서를 참조하여 필요한 규칙을 확인한다. 각 주제의 정본은 이 문서들이고, CLAUDE.md는 같은 내용을 다시 적지 않는다.

| 문서 | 경로 | 참조 시점 |
|------|------|---------|
| 작업 파이프라인 | `harness/WORKFLOW.md` | 모든 작업 시작 전 — Issue 확인, 계획·승인, 구현·커밋, 병합·완료 절차 |
| 백엔드 아키텍처 | `harness/ARCHITECTURE.md` | 새 파일·기능 추가 시 — 계층별 책임과 구성 요소, 모듈 간 호출 방식 |
| 테스트 프로토콜 | `harness/TESTING-GUIDE.md` | 테스트 코드 작성 시 — Base 클래스, Given-When-Then, Edge Case 검증 |
| Issue 작성 규칙 | `harness/ISSUE-GUIDE.md` | Issue 작성·갱신 시 — 요구사항, 설계 결정과 맥락, 완료 조건 |
| 실행 계획 규칙 | `harness/PLAN-GUIDE.md` | PLAN 작성·갱신 시 — 커밋 단위 변경, 검증과 부작용 |

## Project Shape

**DunbarHorizon**은 Spring Boot 3.4.0 / Java 21 REST API이며 헥사고날 아키텍처(Ports & Adapters)를 쓴다. 계층 구조와 어댑터 디렉터리 규칙은 `harness/ARCHITECTURE.md`가 정본이다.

도메인 모듈:
- `account/` — 가입, 로그인, 이메일 인증, OAuth2 Google
- `buzz/` — 콘텐츠 캐스팅(핵심 소셜 기능), MongoDB
- `flag/` — Flag, FlagComment, Memorial
- `social/` — Friendship, 친구 요청, Label, 관계망 분석 (MySQL)
- `notification/` — Firebase FCM 푸시, MongoDB
- `trace/` — 방문 기록
- `global/` — 횡단 관심사: security, DB config, 예외 처리, 어노테이션, 도메인 이벤트, base entity

## Database Wiring

| Database | Config | Base packages |
|----------|--------|---------------|
| MySQL (JPA/Hibernate) | `global/config/database/JpaConfig.java` | `account`, `buzz`, `trace`, `flag`, `notification.adapter.out.persistence.jpa`, `social.adapter.out.persistence.jpa` |
| MongoDB | `global/config/database/MongoConfig.java` | `notification.adapter.out.persistence.mongo`, `buzz.adapter.out.persistence.mongo` |

로컬 기본값:
- MySQL: `localhost:3306/dunbar_horizon`
- MongoDB: `localhost:27017/dunbar_horizon_notification`

## Authentication

- **핵심 원칙:** 증명되기 전에는 신원 키가 아니다. 이메일 소유가 증명된 뒤에만 계정이 생성되므로
  `Auth.verified` 같은 플래그가 존재하지 않고, 로컬과 OAuth가 단일 모델이다.
- **JWT (email/password):** 토큰은 HTTP-only 쿠키(`access_token`, `refresh_token`)로 내려간다. HMAC-SHA512. refresh 기본 TTL 7일(604800초).
- **OAuth2 (Google):** `CustomOAuth2UserService`(공급자 `email_verified` 검증) → `OAuth2AuthenticationSuccessHandler`가 JWT 쿠키를 발급한다.
- **로그인 실패는 세 경우 모두 동일한 401 + 동일 본문**(계정 열거 차단). 사유는 로그로만 남긴다.
- Public endpoints: `POST /api/v1/auth/users`, `POST /api/v1/auth/tokens`, `PATCH/DELETE /api/v1/auth/tokens`, `POST /api/v1/auth/verifications`, `GET /api/v1/auth/verifications/*`, `/oauth2/**`, `/login/oauth2/**`
- 그 밖의 엔드포인트는 인증이 필요하다.
- Roles: `ROLE_USER`, `ROLE_ADMIN`.
- `@CurrentUserId` — 컨트롤러에서 JWT의 사용자 ID를 꺼내는 커스텀 파라미터 어노테이션.

> 로컬 가입은 **사전 인증** 방식이다. 이메일 소유가 증명되기 전에는 `users`/`auths`에
> 행이 생기지 않으며, 비밀번호는 링크 클릭 후에 입력받는다.

## API Conventions

- REST JSON API: 생성 `201`, 성공 `200`, 본문 없음 `204`.
- 인증 엔드포인트: `/api/v1/auth/`
- 기능 엔드포인트: `/api/v1/{domain}/`
- 페이지 응답: `Slice<T>`
- 에러 응답 형태: `{ "error": "ExceptionClassName", "message": "...", "validation": {...} }`

> 엔드포인트 목록은 여기에 적지 않는다. `adapter/in/web`의 컨트롤러가 정본이다.

## Key Domain Concepts

### Social 관계망

- `friendships`는 낮은 사용자 ID와 높은 사용자 ID를 복합 키로 사용한다. 친밀도와 생성일은 공유하고, 관심도·별명·공개 설정은 사용자별 열에 둔다.
- `friend_requests`는 복합 키에 요청 발신자와 수신자를 별도로 기록한다. 라벨 소유자는 `labels`, 멤버십은 `label_members`에 저장한다.
- 조회용 프로필과 계정 상태는 MySQL `users`에서 읽는다. `DunbarCircle`은 관계망 조회 상한 5·15·50·150명을 정한다.
- 기준 친구 추천과 공통 친구 중개인 조회는 각 SQL에서 해당 사용자의 `isRoutable`을 적용한다.

### Buzz Recipients

수신자 지정 방식 셋 (`RecipientType`: `MANUAL`, `LABEL`, `ANCHOR`):
- `ManualRecipientSpec` — 사용자 ID 목록을 직접 지정
- `LabelRecipientSpec` — 라벨 구성원 전체
- `AnchorRecipientSpec` — 앵커 사용자를 통한 친구의 친구

### Domain Events

`global/event/`에 있다:
- `UserInteractionEvent(actorId, targetId, InteractionType)` — MySQL interestScore 갱신
- `MutualInteractionEvent` — 상호 작용 추적
- `NotificationEvent` — FCM 푸시 발행

애그리거트 루트는 `BaseTimeAggregateRoot`를 상속해 `@DomainEvents`로 이벤트를 발행한다.

## Key Patterns

- **Soft delete:** `global/common/SoftDeletable`.
- **Auditing:** `BaseTimeEntity`와 JPA auditing으로 `createdAt`/`updatedAt` 관리.
- **Async:** 알림과 백그라운드 작업에 `@Async` + Spring Retry.
- **Friendship decay:** `FriendshipScoreService`와 `FriendshipDecayPolicy`가 시간 기반 intimacy 감쇠를 처리한다.

## Environment Variables

```
# Required
JWT_SECRET_KEY              # Base64-encoded HMAC-SHA512 secret
JWT_ACCESS_EXPIRATION       # Access token TTL (seconds)
JWT_REFRESH_EXPIRATION      # Refresh token TTL (default: 604800)

OAUTH_GOOGLE_CLIENT_ID
OAUTH_GOOGLE_CLIENT_SECRET

SMTP_USER                   # Gmail address
SMTP_PASSWORD               # Gmail app password

MYSQL_PASSWORD              # MySQL root password

# Optional (have defaults)
MYSQL_URL                   # default: jdbc:mysql://localhost:3306/dunbar_horizon
DB_USERNAME                 # default: root
MONGO_URI                   # default: mongodb://localhost:27017/dunbar_horizon_notification
```

## Spring Profiles

설정은 `src/main/resources/application.yml`에 있다.

- `local` (기본) — CORS는 `http://localhost:3000` 허용. `@Profile("local")` dev 컨트롤러가 여기서만 뜬다.
- `prod` — CORS는 `https://www.dunbarhorizon.com` 허용. JVM: `-Xms256m -Xmx512m`
- `test` — 테스트 전용. `src/test/resources/application-test.yml`
- `perf` — 성능 측정 엔드포인트(`@Profile("perf")`)용

전환: `spring.profiles.active` 또는 JVM 플래그 `-Dspring.profiles.active=prod`.

Dockerfile은 `eclipse-temurin:21-jdk-alpine`을 쓰고 `prod` 프로필을 활성화한다.
