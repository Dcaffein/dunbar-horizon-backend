# PLAN — Account API URL 정리

## 작업 목표

자체 공개 REST API의 버전 규칙을 `/api/v1`으로 통일한다. Account 인증 API의
`/api/auth/**`를 `/api/v1/auth/**`로 옮기고, 사용자 리소스인 프로필 수정과
프로필 이미지 presign은 `/api/v1/users/me/**`로 정렬한다. OAuth/Spring Security
경로(`/oauth2/**`, `/login/oauth2/**`), 루트 경로, `/api/dev/**`는 변경하지 않는다.

## 현황 분석

- `AccountController` 하나가 현재 `/api/auth` 아래의 가입·로그인·로그아웃·재발급·이메일
  검증과 사용자 리소스 두 개를 함께 제공한다.
- `UserController`는 이미 `/api/v1/users` 아래에서 `GET /me`, `GET /search`를 제공한다.
  따라서 `PATCH /users/me`와 `POST /users/me/profile-image/presign`은 이 컨트롤러로
  옮기는 것이 동일 사용자 리소스의 책임과 URL을 함께 정리한다.
- `SecurityConfig`는 `/api/auth/users`, `/tokens`, `/verifications` 및 토큰 갱신·로그아웃,
  검증 토큰 조회를 공개 matcher로 열고 있다. 새 인증 URL에 같은 HTTP 메서드와 공개 범위를
  정확히 이관해야 가입·로그인·재발급 흐름이 유지된다.
- `JwtAuthenticationFilter`에는 `shouldNotFilter()` 같은 URL 예외가 없다. 실패해도
  필터 체인을 계속 진행하는 구조이므로 matcher 변경은 필요 없고, 재발급 URL을 설명하는
  주석과 테스트 설명만 새 경로에 맞춘다.
- `AccountControllerTest`는 모든 기존 `/api/auth/**` 호출과 두 프로필 수정 호출을 포함한다.
  프로필 presign의 웹 경로를 직접 검증하는 테스트는 아직 없다.
- API 문서는 `CLAUDE.md`에만 있으며 공개 엔드포인트, API convention, Account 표가 모두
  이전 경로를 적고 있다. 저장소 및 상위 디렉터리에서 `AGENTS.md`는 발견되지 않았다.

## URL 계약과 호환성 결정

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

기존 `/api/auth/**` controller 별칭은 남기지 않는다. 이는 공개 자체 REST API의 `/api/v1`
통일 목표와 양립하지 않으며, 클라이언트는 목표 URL로 전환해야 한다. 이관 뒤 기존 비인증
요청은 더 이상 permit matcher에 맞지 않아 401, 인증된 요청은 controller mapping 부재로 404가
될 수 있다. OAuth 경로, 루트, `/api/dev/**`의 matcher와 동작은 그대로 둔다.

`AuthCookieManager`는 두 JWT 쿠키를 현재 `Path=/`로 발급·만료하므로 이번 URL 변경만으로
쿠키 전달이 끊기지 않는다. 향후 refresh cookie path를 제한하는 별도 작업을 재개한다면 새
`/api/v1/auth/tokens` 경로를 사용해야 한다.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `src/main/java/com/example/DunbarHorizon/account/adapter/in/web/AccountController.java` | 클래스 매핑을 `/api/v1/auth`로 변경하고 인증·검증 endpoint만 유지한다. |
| `src/main/java/com/example/DunbarHorizon/account/adapter/in/web/UserController.java` | 프로필 수정과 profile-image presign handler 및 의존성을 이관해 `/api/v1/users/me/**`를 제공한다. |
| `src/main/java/com/example/DunbarHorizon/global/security/SecurityConfig.java` | 공개 인증 matcher를 새 `/api/v1/auth/**` 계약의 동일 메서드·깊이로 교체한다. OAuth, 루트, dev matcher는 보존한다. |
| `src/main/java/com/example/DunbarHorizon/global/security/JwtAuthenticationFilter.java` | 재발급 경로를 설명하는 주석만 새 URL로 갱신한다. 필터 동작은 바꾸지 않는다. |
| `src/test/java/com/example/DunbarHorizon/account/adapter/in/web/AccountControllerTest.java` | 인증·검증 요청 URL을 새 계약으로 교체한다. |
| `src/test/java/com/example/DunbarHorizon/account/adapter/in/web/UserControllerTest.java` | 프로필 수정 테스트를 사용자 리소스 책임에 맞게 이관하고, profile-image presign의 새 URL·위임을 검증한다. |
| `src/test/java/com/example/DunbarHorizon/global/security/JwtAuthenticationFilterTest.java` | 재발급 경로 설명을 새 URL로 갱신한다. |
| `CLAUDE.md` | public endpoint 목록, API convention, Account API 표를 새 `/api/v1/auth` 및 `/api/v1/users` 계약으로 갱신한다. |

## 구현 방향과 예상 영향

- 컨트롤러 이동은 web adapter 내부의 책임 정리이며 UseCase, DTO, 응답 상태·본문, JWT cookie
  발급 방식은 바꾸지 않는다.
- SecurityConfig의 인증 API 공개 범위는 기존과 같게 유지한다. 프로필 관련 두 새 URL은 인증이
  계속 필요하다.
- Swagger/OpenAPI는 annotation 기반 별도 경로 정의나 정적 API 문서가 없어 controller mapping
  변경을 따라간다. 과거 task 문서는 이력 자료이므로 수정하지 않는다.
- 프론트엔드, OAuth redirect/callback, health check, dev/perf API와 도메인·DB 스키마는 범위 밖이다.

## 테스트 전략

승인 후 controller web slice 테스트로 새 인증·검증 URL, 인증된 사용자 리소스 URL, presign 위임과
기존 HTTP 응답 계약을 검증한다. 또한 JWT filter 단위 테스트로 만료 access token이 새 토큰
재발급 흐름에서도 체인을 계속 통과한다는 기존 보장을 유지한다.

```powershell
$env:JAVA_HOME='C:\\Users\\TFX5470H\\.jdks\\corretto-17.0.15'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat test --no-daemon --rerun-tasks --tests '*AccountControllerTest' --tests '*UserControllerTest' --tests '*JwtAuthenticationFilterTest'
```

## 구현 및 검증 결과

- `ai/refactor-account-api-url-cleanup` 브랜치에서 Account 인증 API를 `/api/v1/auth/**`로
  이관하고, 프로필 수정과 이미지 presign handler를 `UserController`로 옮겨
  `/api/v1/users/me/**`로 제공하도록 구현했다.
- 공개 Security matcher는 새 인증 경로로 같은 HTTP 메서드와 접근 범위를 유지했다. OAuth,
  루트, `/api/dev/**` matcher 및 JWT filter 동작은 변경하지 않았다.
- Account·User controller 테스트를 새 URL로 갱신하고 profile-image presign의 새 경로와 포트
  위임을 검증하는 테스트를 추가했다. `CLAUDE.md`의 Account API 문서도 새 계약으로 갱신했다.
- 경로 검색 결과, 이력 task와 계획 문서를 제외한 실행 코드·테스트·현재 API 문서에는
  `/api/auth` 참조가 남지 않았다. `git diff --check`도 통과했다.
- 새 worktree의 Java 21 toolchain·의존성 초기화와 Gradle 캐시 파일 권한을 정리한 뒤 선택
  테스트를 재실행했다. `AccountControllerTest` 13개, `UserControllerTest` 7개,
  `JwtAuthenticationFilterTest` 4개와 glob에 함께 포함된 `SocialUserControllerTest` 2개,
  총 26개가 모두 통과했다.

---

# PLAN — Friend Request 수신 상태 조회 완화

## 1. 작업 목표

`received` 친구 요청 조회에서 `PENDING`과 `HIDDEN`만 허용하던 예외 검증을 제거한다. `sent`의 status 금지는 유지해, 보낸 사람이 수신자의 숨김 처리를 탐색할 수 없게 한다.

## 2. 현황 분석

- `FriendRequestQueryService`는 `received` 요청에서 status가 생략되면 `PENDING`을 기본값으로 사용한다.
- 현재는 `PENDING`, `HIDDEN` 외 상태를 전달하면 `FriendRequestInvalidException`을 던져 400을 반환한다.
- 수락된 요청은 수락 흐름에서 삭제되므로 `received&status=ACCEPTED`는 제한을 제거하면 빈 목록을 반환한다.
- `sent`는 status를 받지 않고 저장소에서 `requesterId + PENDING`만 조회한다. status 파라미터 금지는 보낸 사람이 `HIDDEN` 상태를 탐색하지 못하게 하는 개인정보 규칙이므로 유지한다.

## 3. 변경 파일

| 파일 | 변경 |
|---|---|
| `src/main/java/com/example/DunbarHorizon/social/application/service/FriendRequestQueryService.java` | `received` status whitelist 예외를 제거하고 전달된 enum 상태로 수신 요청을 조회한다. sent 분기와 기본 PENDING 정책은 유지한다. |
| `src/test/java/com/example/DunbarHorizon/social/application/FriendRequestQueryServiceTest.java` | `received + ACCEPTED`가 예외 없이 receiver/status 조건으로 조회되는지 검증한다. 기존 예외 테스트는 변경한다. |

## 4. 구현 방향

- `queryStatus`는 계속 `status == null ? PENDING : status`로 결정한다.
- `direction == SENT`일 때의 status 거부와 `findSentRequests()`의 `PENDING` 고정 조건은 변경하지 않는다.
- `RECEIVED`는 `queryStatus`를 그대로 `findAllByReceiver_IdAndStatus`에 전달한다.
- API의 잘못된 enum 문자열은 기존 Spring binding 오류 처리에 맡긴다.

## 5. 예상 사이드 이펙트

- `GET /api/v1/friend-requests?direction=received&status=ACCEPTED`의 결과가 400에서 200 빈 배열(현재 데이터 모델 기준)로 변경된다.
- 숨김 요청 탐색 방지 정책은 `sent` 분기와 PENDING 고정 저장소 조회로 그대로 보존된다.
- API URL, request/response DTO, domain 상태 전이 규칙은 변경하지 않는다.

## 6. 테스트 전략

사용자가 코드 완료를 확인한 뒤 승인하면 다음 테스트를 실행한다.

```powershell
$env:JAVA_HOME='C:\\Users\\TFX5470H\\.jdks\\corretto-17.0.15'
$env:Path="$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat test --no-daemon --rerun-tasks --tests '*FriendRequestQueryServiceTest'
```

수신 `PENDING` 기본값, `HIDDEN` 조회, `ACCEPTED` 전달 조회, sent status 거부 및 sent PENDING 고정 조회를 검증한다.

---

# PLAN — 프로젝트 README 작성 (2026-08-31)

## 작업 목표와 승인 범위

사용자가 로컬 프로젝트의 `README.md` 작성을 명시적으로 요청했다.
앞서 제안한 서비스 소개, 바로가기, 대표 사례, 구조, 실행·테스트, 한계의 구성으로 작성한다.
이 문서 작업만 승인된 범위이며, Outbox 동작과 장애 테스트 구현, 공개 Notion 갱신, 원격 푸시는 포함하지 않는다.

## 현황 분석

- 저장소 루트에 README가 없고 상세 기술서는 외부 Notion에 있다.
- Java 21과 Gradle Wrapper를 사용한다. 단위 테스트와 Testcontainers 기반 DB 테스트가 있다.
- 전체 애플리케이션 실행에는 DB·Redis와 OAuth·메일·Firebase·S3 설정이 필요하다.
- 루트 `docker-compose.yml`은 ECR 이미지와 외부 네트워크를 사용하는 배포용 파일이다.
- Outbox의 수신 처리와 재시도 단위 테스트는 있지만, 두 DB의 커밋 경계 및 장애 복구 통합 검증은 아직 없다.

## 변경 파일과 구현 방향

| 파일 | 변경 |
|---|---|
| `README.md` | 소개, 기술 문서와 코드·테스트 연결, 저장소 구조, 실행·테스트 전제조건과 현재 한계를 작성한다. |
| `PLAN.md` | 기존 작업 기록을 보존하고 이번 문서 작업 범위를 추가한다. |

성능 수치는 합성 부하 조건과 함께 적고, 검증되지 않은 장애 복구·실사용자·협업 경험을 주장하지 않는다.
프로덕션 자격증명이나 로컬 개인 경로를 문서에 포함하지 않는다.

## 영향 및 검증

- 실행 코드·설정·API의 변경은 없다.
- 문서의 저장소 내부 링크와 실행 명령을 실제 파일과 대조한다.
- Docker 없이 실행하도록 안내한 기존 단위 테스트 명령만 확인하며, 전체 통합 테스트나 배포는 실행하지 않는다.

## 검증 결과

- README의 저장소 내부 링크 25개가 모두 존재하는 파일 또는 디렉터리를 가리킴을 확인했다.
- 설치된 JDK 21을 사용해 README의 단위 테스트 명령을 실행했다. `FriendshipTest` 7개와 `SocialNetworkExposurePolicyTest` 2개, 총 9개가 실패·오류 없이 통과했다.
- 전체 테스트·DB 장애 주입·배포는 실행하지 않았다. 실행 코드와 기존 테스트 코드는 변경하지 않았다.
- 별도 문서 검토에서 기술 스택·실행 전제조건·검증 범위와 실제 코드의 일치를 확인했다.

---

# PLAN — User Sync 커밋 순서 수정과 코드 안내 README (2026-08-31)

## 작업 목표와 승인 범위

사용자의 후속 지시에 따라 Neo4j 커밋 순서를 먼저 수정한 뒤 장애·유실·재처리 테스트를 붙인다.
README는 헥사고날 구조, 코드 위치, 요청 및 이벤트 흐름을 설명하는 개발자 안내로 개편한다.
도메인 정책, 이벤트 스키마, 재시도 횟수·주기, 배포 구성은 변경하지 않는다. 원격 푸시는 하지 않는다.

## 현황 분석

- `SocialUserEventListener.onUserSync()`의 Neo4j 트랜잭션은 메서드 반환 후 커밋된다.
- 현재 메서드 내부의 완료 이벤트가 MySQL 완료 처리를 동기 호출하므로 Neo4j 커밋보다 먼저 COMPLETED가 저장될 수 있다.
- 메서드 내부 catch는 프록시가 수행하는 커밋 실패를 잡지 못한다.
- 기존 Mockito 테스트는 실제 Spring 프록시와 두 DB의 커밋 경계를 검증하지 않는다.
- 기존 README는 사례 소개 비중이 높고 포트·어댑터의 실제 연결과 실행 흐름 안내가 부족하다.

## 변경 파일과 구현 방향

| 파일 | 변경 |
|---|---|
| `social/application/service/SocialUserSyncCommandService.java` | 기존 그래프 변경을 이동하고 Neo4j REQUIRES_NEW 트랜잭션을 책임진다. |
| `social/application/eventListener/SocialUserEventListener.java` | 비동기 수신 → 별도 서비스 프록시 호출 → 성공 반환 후 완료 이벤트 발행. |
| `account/application/eventListener/UserOutboxEventListener.java` | 완료 처리를 명시적인 MySQL REQUIRES_NEW 트랜잭션으로 수행한다. |
| 관련 단위 테스트 | 그래프 변경과 이벤트 조율 책임에 맞게 분리하고 실패 시 완료 미발행을 검증한다. |
| 교차 DB 통합 테스트 및 `support` 테스트 지원 | 커밋 전 대기·실패, 최초 이벤트 유실, 완료 이벤트 유실과 실제 재시도를 검증한다. |
| `README.md` | 실제 패키지와 포트·어댑터 연결, HTTP 및 Outbox 흐름, 실행·테스트 안내를 중심으로 재구성한다. |

## 영향과 테스트 전략

- 완료 처리는 Neo4j 커밋 성공 이후에만 실행된다. 완료 처리 실패·유실 시 PENDING이 남아 기존 재시도의 대상이 된다.
- 그래프 처리 예외는 트랜잭션 밖에서 잡으므로 Neo4j 롤백이 먼저 일어난다.
- 기존 슬라이스 테스트의 자동 롤백은 AFTER_COMMIT 검증에 부적합하다. `support/TestContainerConfig`를 재사용하는 좁은 Spring 통합 컨텍스트에서 실제 트랜잭션을 커밋한다.
- 장애 주입은 테스트 전용 코드에만 두고, 실제 저장소와 Spring 프록시를 사용한다. 커밋 경계 예외와 이벤트 전달 누락을 재현하며 실제 네트워크 단절·프로세스 종료를 검증했다고 주장하지 않는다.
- 비동기 작업은 명시적인 완료 신호와 제한 시간으로 기다린다. 테스트가 만든 데이터만 정리한다.
- README 내부 링크와 설명을 실제 코드와 대조한다.

## 구현 및 검증 상태

- Neo4j 트랜잭션을 별도 서비스로 분리했고, 서비스 프록시 성공 반환 후에만 완료 이벤트를 발행하도록 변경했다. 완료 처리는 명시적인 MySQL REQUIRES_NEW를 사용한다.
- 기존 그래프 동기화 단위 테스트 8개를 서비스 책임으로 옮기고, 리스너 조율 테스트 4개를 추가했다. Outbox 기록 5개·재시도 2개와 합쳐 관련 단위 테스트 19개가 통과했다.
- 실제 커밋 전 대기, 커밋 직전 예외와 재시도, 최초 이벤트 유실, 완료 이벤트 유실과 중복 방지의 통합 테스트 4개를 작성했다. `support`의 전용 Base 및 좁은 Spring 컨텍스트를 사용하며 공용 Testcontainers를 재사용한다.
- 통합 테스트를 포함한 전체 테스트 소스 컴파일은 성공했다. 선택한 23개 중 단위 19개는 통과했으나, 통합 4개는 Docker 엔진 미연결로 Testcontainers 컨텍스트 초기화에서 실패했다. 실제 DB assertion과 복구 검증은 미실행 상태다.
- README를 코드 구조, 포트·어댑터 매핑, HTTP 조회·변경 흐름, 사용자 동기화 흐름 중심으로 개편했다. 테스트 미검증 범위를 구분해 명시했다.
- 독립 검토에서 실행 코드의 커밋·예외 경계와 README 주요 설명을 확인했다. Docker 준비 후 통합 테스트를 재실행하고 통과한 뒤 커밋한다. 현재 변경은 미커밋 상태다.
