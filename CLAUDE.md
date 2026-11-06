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

> 통합 테스트는 Docker가 필요하다(Testcontainers: MySQL, Neo4j, MongoDB).
> 컨테이너 이미지와 버전의 정본은 `src/test/java/com/example/DunbarHorizon/support/TestContainerConfig.java`다.

## 문서 지도 (Document Map)

작업 시작 전 아래 문서를 참조하여 필요한 규칙을 확인한다. 각 주제의 정본은 이 문서들이고, CLAUDE.md는 같은 내용을 다시 적지 않는다.

| 문서 | 경로 | 참조 시점 |
|------|------|---------|
| 작업 파이프라인 | `harness/WORKFLOW.md` | 모든 작업 시작 전 — 계획(PLAN.md) 작성, 승인, 브랜치, 구현, 커밋 순서 |
| 아키텍처 & 코딩 컨벤션 | `harness/ARCHITECTURE.md` | 새 파일·기능 추가 시 — 계층 분리, DDD/Hexagonal 규칙, 도메인 간 통신 방식 |
| 테스트 프로토콜 | `harness/TESTING-GUIDE.md` | 테스트 코드 작성 시 — Base 클래스, Given-When-Then, Edge Case 검증 |
| 태스크 문서 규칙 | `harness/TASK-SPEC-GUIDE.md` | 새 태스크 문서 작성 시 — 섹션 구성과 각 섹션에 쓸 것 |

## Project Shape

**DunbarHorizon**은 Spring Boot 3.4.0 / Java 21 REST API이며 헥사고날 아키텍처(Ports & Adapters)를 쓴다. 계층 구조와 어댑터 디렉터리 규칙은 `harness/ARCHITECTURE.md`가 정본이다.

도메인 모듈:
- `account/` — 가입, 로그인, 이메일 인증, OAuth2 Google
- `buzz/` — 콘텐츠 캐스팅(핵심 소셜 기능), MongoDB
- `flag/` — Flag, FlagComment, Memorial
- `social/` — Friendship, 친구 요청, Label, 관계망 분석 (Neo4j)
- `notification/` — Firebase FCM 푸시, MongoDB
- `trace/` — 방문 기록
- `global/` — 횡단 관심사: security, DB config, 예외 처리, 어노테이션, 도메인 이벤트, base entity

## Database Wiring

어느 특성이면 어느 저장소를 쓰는지는 `harness/ARCHITECTURE.md` §2에 있다. 여기에는 실제 배선만 적는다.

| Database | Config | Base packages |
|----------|--------|---------------|
| MySQL (JPA/Hibernate) | `global/config/database/JpaConfig.java` | `account`, `buzz`, `trace`, `flag`, `notification.adapter.out.persistence.jpa`, `social.adapter.out.persistence.jpa` |
| Neo4j | `global/config/database/Neo4jConfig.java` | `social` |
| MongoDB | `global/config/database/MongoConfig.java` | `notification.adapter.out.persistence.mongo`, `buzz.adapter.out.persistence.mongo` |

Neo4j 스키마와 데이터 변경은 `src/main/resources/neo4j/migrations/`의 Cypher 파일이 앱 기동 때 실행된다(Neo4j-Migrations).

로컬 기본값:
- MySQL: `localhost:3306/dunbar_horizon`
- Neo4j: `bolt://localhost:7687`, database: `dev` (Neo4j Desktop)
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

### Social Graph (Neo4j)

**노드:**
- `SocialUser` (label: `USER_REFERENCE`) — account 도메인에서 동기화된다
- `Label` — 사용자가 만든 친구 그룹
  - 속성: `name`, `exposure`

**관계:**
- `(low)-[:FRIENDS_WITH]->(high)` — 친구 쌍마다 하나. 낮은 id의 사용자가 시작 노드다
  - 공통 속성: `id`, `createdAt`, `intimacy`
  - 사용자별 속성: `low`, `high` 접두사 + `FriendAlias`, `InterestScore`, `LastInteractedAt`, `IsMuted`, `IsRoutable`
- `(user)-[:OWNS_LABEL]->(label)`
- `(user)-[:ATTACHED_TO]->(label)`

**DunbarCircle** (`social/domain/friend/DunbarCircle.java`):
```java
SUPPORT(5), SYMPATHY(15), KINSHIP(50), DUNBAR(150)
```
프론트엔드 슬라이더 값을 관계망 조회 크기 상한으로 매핑한다. 이 150은 시각화 상한이며 실제 친구 수 상한이 아니다.

**관계망 조회 규칙:**
- 기본 관계망: intimacy 상위 N명 + 그들 사이의 상호 연결
- Stranger Quota: 노출되는 공통 친구는 최대 5명 (프라이버시)
- `isRoutable = false` 친구는 다른 사람의 관계망 뷰에서 숨는다

Neo4j 접근은 두 층이다. Cypher는 `neo4j/springData/`의 Spring Data 리포지터리에서 `@Query`로 쓰는 것이 기본이고, SDN이 매핑하지 못하는 경우에만 `neo4j/`의 어댑터가 `Neo4jClient`로 직접 실행한다. `Friendship`은 관계라서 SDN 엔티티가 아니고, 어댑터가 관계와 양 끝 노드로 조립한다. `SocialNetworkRepositoryAdapter`가 `apoc.path.expandConfig`를 쓰므로 Neo4j에 APOC 플러그인이 필요하다.

### Buzz Recipients

수신자 지정 방식 셋 (`RecipientType`: `MANUAL`, `LABEL`, `ANCHOR`):
- `ManualRecipientSpec` — 사용자 ID 목록을 직접 지정
- `LabelRecipientSpec` — 라벨 구성원 전체
- `AnchorRecipientSpec` — 앵커 사용자를 통한 친구의 친구

### Domain Events

`global/event/`에 있다:
- `UserActivatedEvent`, `UserDeactivatedEvent` — 계정 생명주기
- `UserInteractionEvent(actorId, targetId, InteractionType)` — Neo4j interestScore 갱신
- `MutualInteractionEvent` — 상호 작용 추적
- `NotificationEvent` — FCM 푸시 발행

애그리거트 루트는 `BaseTimeAggregateRoot`를 상속해 `@DomainEvents`로 이벤트를 발행한다.

## Key Patterns

- **Soft delete:** `global/common/SoftDeletable`.
- **Auditing:** `BaseTimeEntity`(JPA) + Neo4j auditing으로 `createdAt`/`updatedAt` 관리.
- **Async:** 알림과 백그라운드 작업에 `@Async` + Spring Retry.
- **Cross-DB sync:** `SocialUserSyncHelper`가 계정 이벤트에 맞춰 MySQL ↔ Neo4j `SocialUser` 노드를 맞춘다.
- **Friendship decay:** `FriendshipDecayService` / `FriendshipDecayPolicy`가 시간 기반 intimacy 감쇠를 처리한다.

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
NEO4J_PASSWORD

# Optional (have defaults)
MYSQL_URL                   # default: jdbc:mysql://localhost:3306/dunbar_horizon
DB_USERNAME                 # default: root
NEO4J_URI                   # default: bolt://localhost:7687
MONGO_URI                   # default: mongodb://localhost:27017/dunbar_horizon_notification
```

## Spring Profiles

설정은 `src/main/resources/application.yml`에 있다.

- `local` (기본) — Neo4j Cypher 로깅 DEBUG, CORS는 `http://localhost:3000` 허용. `@Profile("local")` dev 컨트롤러가 여기서만 뜬다.
- `prod` — CORS는 `https://www.dunbarhorizon.com` 허용. JVM: `-Xms256m -Xmx512m`
- `test` — 테스트 전용. `src/test/resources/application-test.yml`
- `perf` — 성능 측정 엔드포인트(`@Profile("perf")`)용

전환: `spring.profiles.active` 또는 JVM 플래그 `-Dspring.profiles.active=prod`.

Dockerfile은 `eclipse-temurin:21-jdk-alpine`을 쓰고 `prod` 프로필을 활성화한다.
