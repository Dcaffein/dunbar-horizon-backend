# DunbarHorizon

친구 관계를 네트워크 맵으로 보여주고, 그 위에서 소통하고 함께할 활동을 만드는 SNS의 백엔드입니다.
관심은 각자의 것으로, 친밀도는 두 사람의 관계로 모델링하고, 친밀도와 노출 정책에 따라 관계망의 탐색 범위를 정합니다.

이 저장소는 Java 21 / Spring Boot 3.4.0 기반 REST API입니다. 하나의 Spring Boot 애플리케이션 안에서 기능별 도메인을 나누고, MySQL·MongoDB·Redis를 역할에 맞게 사용합니다.

[서비스](https://www.dunbarhorizon.link) · [기술 문서 및 체험 안내](https://valiant-vest-6c8.notion.site/DunbarHorizon-3776b857043480998e6fdebaba6ba662) · [프론트엔드 저장소](https://github.com/Dcaffein/dunbar-horizon-frontend)

## 코드 구조

기능을 먼저 나누고, 각 기능 안에 헥사고날 아키텍처의 포트와 어댑터를 둡니다. 실행 진입점은 [DunbarHorizonBackendApplication](src/main/java/com/example/DunbarHorizon/DunbarHorizonBackendApplication.java)입니다.

```text
src/main/java/com/example/DunbarHorizon/
├── account / social / buzz / flag / trace / notification
│   ├── adapter/
│   │   ├── in/web/              HTTP Controller, 요청·응답 DTO
│   │   ├── in/scheduler/        주기 작업 진입점
│   │   └── out/                 DB·Redis·다른 도메인·외부 서비스 연결
│   ├── application/
│   │   ├── port/in/            외부에 제공하는 UseCase 인터페이스
│   │   ├── port/out/           조회·캐시·외부 기능에 필요한 인터페이스
│   │   ├── service/            유스케이스 조율, 트랜잭션 경계
│   │   ├── eventListener/      이벤트 수신과 후속 작업
│   │   └── dto/                유스케이스 입력·조회 결과
│   └── domain/                 모델, 정책, 예외, 이벤트, 저장소 인터페이스
└── global/
    ├── security/               JWT·쿠키·인증 처리
    ├── annotation/             @CurrentUserId
    ├── config/                 DB·Redis·비동기·외부 연동 설정
    ├── event/                  도메인 사이에 전달하는 이벤트
    └── exception/              공통 예외와 HTTP 오류 응답
```

위 트리는 공통적인 배치입니다. 모든 도메인이 모든 하위 폴더를 갖지는 않으며, Social은 `domain/friend`, `domain/label`처럼 개념별로 한 번 더 나눕니다.

### 포트와 구현을 찾는 방법

| 찾는 코드 | 인터페이스·정책 | 연결되는 구현 |
|---|---|---|
| 관계망 조회 기능 | [SocialNetworkQueryUseCase](src/main/java/com/example/DunbarHorizon/social/application/port/in/SocialNetworkQueryUseCase.java) | [SocialNetworkQueryService](src/main/java/com/example/DunbarHorizon/social/application/service/SocialNetworkQueryService.java) |
| 관계망 조회 저장소 | [SocialNetworkRepository](src/main/java/com/example/DunbarHorizon/social/application/port/out/SocialNetworkRepository.java) | [SocialNetworkMysqlRepository](src/main/java/com/example/DunbarHorizon/social/adapter/out/persistence/mysql/SocialNetworkMysqlRepository.java) — SQL과 결과 매핑 |
| 계정 도메인 저장소 | [UserRepository](src/main/java/com/example/DunbarHorizon/account/domain/repository/UserRepository.java) | [UserRepositoryAdapter](src/main/java/com/example/DunbarHorizon/account/adapter/out/persistence/UserRepositoryAdapter.java) → Spring Data JPA |
| 네트워크 캐시 무효화 | [SocialNetworkCacheManager](src/main/java/com/example/DunbarHorizon/social/application/port/out/SocialNetworkCacheManager.java) | [SocialNetworkCacheAdapter](src/main/java/com/example/DunbarHorizon/social/adapter/out/redis/SocialNetworkCacheAdapter.java) → Redis 키 삭제 |
| Flag가 필요로 하는 친구 여부 | [FriendshipChecker](src/main/java/com/example/DunbarHorizon/flag/domain/flag/FriendshipChecker.java) | [FlagUserAdapter](src/main/java/com/example/DunbarHorizon/flag/adapter/out/user/FlagUserAdapter.java) → Social의 입력 포트 |

Controller는 UseCase를 호출하고, 서비스는 저장소·외부 기능의 인터페이스를 사용합니다. DB나 다른 도메인을 아는 구체 클래스는 출력 어댑터이며, Spring이 인터페이스에 구현체를 주입합니다.

출력 포트는 `application/port/out`과 `domain/**/repository`에 있습니다. 도메인 협력 인터페이스는 해당 도메인에서 정의합니다.

### 도메인과 저장소의 책임

| 패키지 | 담당 기능 | 저장소 |
|---|---|---|
| [account](src/main/java/com/example/DunbarHorizon/account) | 가입·인증, 프로필 | MySQL; 가입 대기 토큰은 Redis |
| [social](src/main/java/com/example/DunbarHorizon/social) | 친구·친구 요청·라벨, 관계망, 관심도·친밀도 | MySQL |
| [buzz](src/main/java/com/example/DunbarHorizon/buzz) | 수신 범위를 정한 콘텐츠 공유와 답글 | MongoDB |
| [flag](src/main/java/com/example/DunbarHorizon/flag) | 활동 모집·참여·초대, 활동 이후 회포 | MySQL |
| [trace](src/main/java/com/example/DunbarHorizon/trace) | 프로필 방문과 상호 방문 판정 | MySQL |
| [notification](src/main/java/com/example/DunbarHorizon/notification) | 기기 토큰, 알림 이력, Firebase FCM 발송 | 토큰은 MySQL, 이력은 MongoDB |

기본 `@Transactional`은 [JpaConfig](src/main/java/com/example/DunbarHorizon/global/config/database/JpaConfig.java)의 MySQL 트랜잭션 매니저를 사용합니다.

## 요청 흐름 따라가기

인증된 요청에서는 [JwtAuthenticationFilter](src/main/java/com/example/DunbarHorizon/global/security/JwtAuthenticationFilter.java)가 쿠키의 JWT를 검증해 `SecurityContext`에 인증 정보를 넣습니다. [CurrentUserIdArgumentResolver](src/main/java/com/example/DunbarHorizon/global/annotation/CurrentUserIdArgumentResolver.java)가 그 사용자 ID를 Controller의 `@CurrentUserId` 인자로 전달합니다. 예외의 HTTP 응답 변환은 [GlobalExceptionHandler](src/main/java/com/example/DunbarHorizon/global/exception/GlobalExceptionHandler.java)에서 확인할 수 있습니다.

### 1. 읽기: 내 관계망 조회

```text
GET /api/v1/network?circleSize=DUNBAR
  → SocialNetworkController.getFriendsNetwork()
  → SocialNetworkQueryUseCase
  → SocialNetworkQueryService
  → SocialNetworkRepository
  → SocialNetworkMysqlRepository → MySQL → List<NodeGraphResult>
```

시작점은 [SocialNetworkController](src/main/java/com/example/DunbarHorizon/social/adapter/in/web/SocialNetworkController.java)입니다. 서비스는 [DunbarCircle](src/main/java/com/example/DunbarHorizon/social/domain/friend/DunbarCircle.java)의 5·15·50·150명 경계와 가지치기 인자를 조회 포트로 전달합니다. 어댑터의 SQL은 친밀도순으로 친구를 먼저 제한하고, 그 경계 안의 연결을 조회해 결과 DTO로 매핑합니다.


별도의 `/api/v1/network/edges` 흐름에서는 [SocialNetworkExposurePolicy](src/main/java/com/example/DunbarHorizon/social/domain/friend/SocialNetworkExposurePolicy.java)가 친구·2-hop 대상별 연결 노출 수를 결정합니다. 기본 관계망의 가지치기와 이 API의 노출 정책은 서로 다른 코드 경로입니다.

### 2. 쓰기와 도메인 간 조회: 활동 참여

`POST /api/v1/flags/{flagId}/participants`는 다음 순서로 처리합니다.

1. [FlagController](src/main/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagController.java)가 [FlagParticipationUseCase](src/main/java/com/example/DunbarHorizon/flag/application/port/in/FlagParticipationUseCase.java)를 호출합니다.
2. [FlagParticipationService](src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationService.java)가 MySQL 트랜잭션 안에서 [FlagParticipationManager](src/main/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationManager.java)에 참여 판단을 맡깁니다.
3. Manager는 `FriendshipChecker`로 호스트와의 친구 여부를 확인합니다. `FlagUserAdapter`가 이 요청을 Social의 [FriendshipQueryUseCase](src/main/java/com/example/DunbarHorizon/social/application/port/in/FriendshipQueryUseCase.java)에 연결합니다.
4. Manager는 Flag를 비관적 잠금으로 읽고 중복 참여와 현재 인원을 확인합니다. [Flag.participate()](src/main/java/com/example/DunbarHorizon/flag/domain/flag/Flag.java)가 호스트 참여 금지·모집 마감·정원 규칙을 검사하고 참여 객체를 만듭니다.
5. 서비스가 도메인의 [FlagRepository](src/main/java/com/example/DunbarHorizon/flag/domain/flag/repository/FlagRepository.java)를 통해 참여자를 저장합니다. [FlagRepositoryAdapter](src/main/java/com/example/DunbarHorizon/flag/adapter/out/persistence/FlagRepositoryAdapter.java)가 JPA에 위임하고, 커밋 후 Controller가 `201`을 반환합니다.

이처럼 다른 도메인의 정보가 즉시 필요할 때는 **소비하는 도메인의 포트 → 연결 어댑터 → 제공하는 도메인의 입력 포트**를 따라갑니다.

## Social 계정 정보와 데이터 이관

Social 조회는 관계에 저장된 사용자 ID를 기준으로 MySQL `users`에서 활성 상태와 표시 정보를 조회합니다. 기존 Neo4j 데이터를 옮길 때는 [독립 이관 도구](scripts/social-cutover/README.md)를 사용합니다.

## 테스트

JDK 21을 설치하고 `JAVA_HOME` 또는 `PATH`에서 해당 Java를 찾을 수 있도록 설정합니다. Gradle은 저장소의 Wrapper를 사용합니다.
테스트는 JUnit 5·Mockito·AssertJ·Testcontainers를 사용하며, `src/test/java`에서 해당 도메인의 테스트를 찾을 수 있습니다.

| 검증하려는 책임 | 읽을 테스트·지원 코드 |
|---|---|
| 도메인의 판단 규칙 | [SocialNetworkExposurePolicyTest](src/test/java/com/example/DunbarHorizon/social/domain/friend/SocialNetworkExposurePolicyTest.java) |
| 유스케이스의 호출·저장 조율 | [FlagParticipationServiceTest](src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationServiceTest.java) — 의존성을 모킹한 단위 테스트 |
| Social의 MySQL 조회·갱신 | [SocialMysqlCutoverIntegrationTest](src/test/java/com/example/DunbarHorizon/social/application/service/SocialMysqlCutoverIntegrationTest.java), [SocialConnectionPathMysqlRepositoryTest](src/test/java/com/example/DunbarHorizon/social/adapter/out/persistence/mysql/SocialConnectionPathMysqlRepositoryTest.java) |
| Controller·보안 인자·응답 | [BaseControllerTest](src/test/java/com/example/DunbarHorizon/support/BaseControllerTest.java)와 도메인별 `adapter/in/web` 테스트 |

DB 슬라이스는 `support`의 `@JpaRepositoryTest`와 `@MongoRepositoryTest`를 사용합니다.

### DB 없이 확인하는 단위 테스트 예시

친구 관계 모델과 네트워크 노출 정책을 검증합니다. 최초 실행 시 Gradle과 의존성 다운로드가 필요할 수 있습니다.

```bash
./gradlew test --tests '*FriendshipTest' --tests '*SocialNetworkExposurePolicyTest'
```

Windows PowerShell에서는 다음과 같이 실행합니다.

```powershell
.\gradlew.bat test --tests '*FriendshipTest' --tests '*SocialNetworkExposurePolicyTest'
```

### 실제 DB를 사용하는 테스트와 전체 테스트

Docker가 실행 중이어야 합니다. [공용 Testcontainers 설정](src/test/java/com/example/DunbarHorizon/support/TestContainerConfig.java)은 MySQL 8.0과 MongoDB 7.0을 시작합니다. S3 어댑터 테스트는 별도의 LocalStack 컨테이너를 사용합니다. 테스트 데이터는 운영 DB와 분리합니다.

```bash
# Social 앱과 중개인 MySQL 조회
./gradlew test --tests '*SocialMysqlCutoverIntegrationTest' --tests '*SocialConnectionPathMysqlRepositoryTest'

# 전체 테스트
./gradlew test

# 테스트를 포함한 빌드
./gradlew build
```

테스트 기본값은 [application-test.yml](src/test/resources/application-test.yml)을 참고합니다. 전체 테스트에는 애플리케이션 컨텍스트를 올리는 테스트도 포함돼 있습니다. 현재 Firebase 초기화와 OAuth·메일 설정이 완전히 격리되어 있지 않으므로, 아래 실행 설정 중 테스트에서 대체되지 않는 값과 Firebase 설정 파일이 필요합니다. **Docker만 설치하면 외부 연동 설정 없이 전체 테스트가 실행되는 구성은 아닙니다.**

테스트 리포트는 `build/reports/tests/test/index.html`에 생성됩니다.

## 애플리케이션 실행

MySQL·MongoDB·Redis를 준비하고, [application.yml](src/main/resources/application.yml)에 맞춰 환경 변수를 주입합니다. 연결 정보와 자격증명은 저장소에 커밋하지 않습니다.

| 설정 | 내용 |
|---|---|
| MySQL | `MYSQL_URL`, `DB_USERNAME`, `MYSQL_PASSWORD`. URL·사용자명에는 로컬 기본값이 있습니다. |
| MongoDB·Redis | `MONGO_URI`, `REDIS_HOST`, `REDIS_PORT`. 로컬 기본값이 있습니다. |
| JWT | `JWT_SECRET_KEY`: HS512에 사용할 64바이트 이상 키를 Base64로 인코딩한 값. `JWT_ACCESS_EXPIRATION`: 초 단위. `JWT_REFRESH_EXPIRATION`: 기본 604800초. |
| Google OAuth | `OAUTH_GOOGLE_CLIENT_ID`, `OAUTH_GOOGLE_CLIENT_SECRET` |
| SMTP | `SMTP_USER`, `SMTP_PASSWORD` |
| S3 | `AWS_IMAGE_BUCKET`, `AWS_S3_REGION`(기본 `ap-northeast-2`). 자격증명은 AWS SDK의 기본 공급자 체인으로 제공합니다. |
| 프론트엔드·쿠키 | `FRONTEND_BASE_URL`(기본 `http://localhost:3000`), `COOKIE_DOMAIN`, `COOKIE_SECURE` |

[FirebaseConfig](src/main/java/com/example/DunbarHorizon/global/config/FirebaseConfig.java)는 시작 시 클래스패스의 `dunbarhorizon-firebase.json`을 읽습니다. 로컬에서는 별도 개발용 Firebase 서비스 계정 파일을 `src/main/resources/dunbarhorizon-firebase.json`에 준비해야 합니다. 이 파일은 Git 추적 제외 대상입니다.

환경 변수는 IDE 실행 설정이나 셸에서 주입합니다. 루트에 `.env` 파일을 만드는 것만으로 `bootRun`에 자동 반영되지는 않습니다.

```bash
./gradlew bootRun
```

Windows에서는 `.\gradlew.bat bootRun`을 사용합니다. 기본 HTTP 포트는 8080이며, MySQL 스키마는 [Flyway 마이그레이션](src/main/resources/db/migration)으로 관리합니다.

루트의 [docker-compose.yml](docker-compose.yml)은 **ECR 이미지와 외부 `dunbar-network`를 사용하는 배포용 구성**입니다. 로컬 개발용 DB 네 개를 한 번에 준비하는 파일은 아닙니다.

## 배포와 현재 검증 범위

[GitHub Actions](.github/workflows/deploy.yml)는 `main` 푸시 시 빌드·테스트를 수행하고, 성공한 경우 이미지와 배포 파일을 ECR·S3에 올린 뒤 CodeDeploy에 배포를 요청합니다. 실제 컨테이너 교체는 EC2의 [배포 스크립트](scripts/deploy.sh)가 수행합니다.

- 성능 수치는 별도 실험 환경의 합성 부하 결과입니다. 실사용자 규모나 운영 트래픽 처리 경험으로 표현하지 않습니다.
- 앱의 MySQL 전환과 데이터 이관 도구는 로컬에서 검증합니다. 기존 환경의 데이터를 옮기려면 새 앱을 배포하기 전에 [이관 절차](scripts/social-cutover/README.md)를 실행합니다.
- 현재 배포는 컨테이너 교체 중 짧은 중단을 허용합니다. `latest` 태그를 사용하며, 이전 버전으로의 복구 절차는 후속 과제입니다.

## 설계 배경과 작업 기록

- [기술 문서](https://valiant-vest-6c8.notion.site/DunbarHorizon-3776b857043480998e6fdebaba6ba662): 관계망 모델, 쿼리 실행 계획과 캐시 성능 실험, 대안 비교. 성능 실험은 k6 합성 부하이며 조건과 측정 근거를 함께 기록했습니다.
- 연결 중개인 조회의 범위와 응답은 [설계 문서](https://github.com/Dcaffein/dunbar-horizon-backend/blob/5b47eac255f2873471d313f08989457f9291a532/harness/done/task-105-connection-path-scope.md)에 있습니다. 상한·정렬·노출 정책은 [MySQL 통합 테스트](src/test/java/com/example/DunbarHorizon/social/adapter/out/persistence/mysql/SocialConnectionPathMysqlRepositoryTest.java)에서 검증합니다.
- AI 에이전트 작업은 [작업 절차](harness/WORKFLOW.md), [아키텍처 규칙](harness/ARCHITECTURE.md), [테스트 규칙](harness/TESTING-GUIDE.md), [Issue 작성 규칙](harness/ISSUE-GUIDE.md)을 따릅니다.
