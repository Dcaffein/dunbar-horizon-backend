# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

All Gradle commands should be run from the `DunbarHorizonBe/` subdirectory.

```bash
# Build
./gradlew build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test
```

> Note: Tests are currently disabled in `build.gradle` (line 68: `enabled = false`). Remove that line to re-enable them.

## Architecture

**DunbarHorizon** is a Spring Boot 3.4.0 / Java 21 REST API using **hexagonal architecture** (Ports & Adapters).

### Module Structure

Each domain module follows this layered pattern:
```
{domain}/
├── adapter/in/web/      # REST Controllers, request DTOs
├── adapter/out/         # Persistence adapters, external service adapters (JWT, email, etc.)
├── application/         # Use case services, input/output ports
└── domain/              # Entities, domain exceptions, repository interfaces
```

Domain modules:
- `account/` — User signup, login, email verification, OAuth2 Google
- `buzz/` — Content casting/sharing (the core social feature)
- `flag/` — Content moderation
- `social/` — Friendships, friend requests, labels, social network analysis
- `notification/` — Push notifications via Firebase (FCM)
- `trace/` — User activity tracing
- `global/` — Cross-cutting: security config, DB config, exception handling, custom annotations, domain events, base entities

### Multi-Database Design

The app uses three databases, each optimized for its data type:

| Database | Purpose | Config File |
|----------|---------|-------------|
| MariaDB (JPA/Hibernate) | Relational data: users, casts, flags, traces | `global/config/JpaConfig.java` |
| Neo4j | Graph data: friendship networks, social connections | `global/config/Neo4jConfig.java` |
| MongoDB | Document data: push notification history | `global/config/MongoConfig.java` |

Local defaults: MariaDB on `localhost:3306/dunbarhorizon_db`, Neo4j on `bolt://localhost:7687/dunbarhorizon`, MongoDB on `localhost:27017/dunbarhorizon_notification`.

### Authentication

- **JWT (email/password):** Tokens stored as HTTP-only cookies (`access_token`, `refresh_token`). HMAC-SHA512 signature. Refresh token has 7-day TTL.
- **OAuth2 (Google):** `CustomOAuth2UserService` maps the Google profile; `OAuth2AuthenticationSuccessHandler` issues JWT cookies.
- Public endpoints: `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**`. All others require authentication.
- Roles: `ROLE_USER`, `ROLE_ADMIN`.
- `@CurrentUserId` is a custom annotation that resolves the authenticated user's ID in controller method parameters.

### API Conventions

- REST JSON API; creation returns `201`, success `200`, no-content `204`.
- Auth endpoints: `/api/auth/`
- Feature endpoints: `/api/v1/{domain}/`
- Paginated responses use Spring Data `Slice<T>`.
- Error responses: `ErrorResponse` with `errorName`, `message`, and validation errors.

### Key Patterns

- **Soft delete:** Entities implement `Deletable` / `SoftDeletable` interfaces.
- **Auditing:** `BaseTimeEntity` provides `createdAt`/`updatedAt` for both JPA and Neo4j entities.
- **Domain events:** `UserCreatedEvent`, `UserInteractionEvent` etc. under `global/event/`.
- **Async:** `@Async` + Spring Retry used for notifications and background tasks.

## Environment Variables

These must be set before running:

```
JWT_SECRET_KEY              # Base64-encoded HMAC-SHA512 secret
JWT_ACCESS_EXPIRATION       # Access token expiration (seconds)
JWT_REFRESH_EXPIRATION      # Refresh token expiration (default: 604800)

OAUTH_GOOGLE_CLIENT_ID
OAUTH_GOOGLE_CLIENT_SECRET

SMTP_USER                   # Gmail address
SMTP_PASSWORD               # Gmail app password

MARIADB_PASSWORD
NEO4J_PASSWORD
```

## Spring Profiles

- `local` (default) — uses `application-local.properties`; Neo4j Cypher logging enabled; frontend at `http://localhost:3000`
- `prod` — uses `application-prod.properties`; frontend at `https://www.dunbarhorizon.com`

Switch profiles via `spring.profiles.active` in `application.properties` or as a JVM flag.

## Testing

Integration tests use Testcontainers (MariaDB, Neo4j, MongoDB) — Docker must be running. Test classes are in `src/test/java/com/example/DunbarHorizon/`.
