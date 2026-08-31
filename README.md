# DunbarHorizon

친구 관계를 네트워크 맵으로 보여주고, 그 위에서 소통하고 함께할 활동을 만드는 SNS의 백엔드입니다.
관심은 각자의 것으로, 친밀도는 두 사람의 관계로 모델링하고, 친밀도와 노출 정책에 따라 관계망의 탐색 범위를 정합니다.

이 저장소는 Java 21 / Spring Boot 3.4.0 기반 REST API입니다. 하나의 Spring Boot 애플리케이션 안에서 기능별 도메인을 나누고, MySQL·Neo4j·MongoDB·Redis를 역할에 맞게 사용합니다.

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
    ├── annotation/             @CurrentUserId, @Neo4jTransactional
    ├── config/                 DB·Redis·비동기·외부 연동 설정
    ├── event/                  도메인 사이에 전달하는 이벤트
    └── exception/              공통 예외와 HTTP 오류 응답
```

위 트리는 공통적인 배치입니다. 모든 도메인이 모든 하위 폴더를 갖지는 않으며, Social은 `domain/friend`, `domain/label`처럼 개념별로 한 번 더 나눕니다.

### 포트와 구현을 찾는 방법

| 찾는 코드 | 인터페이스·정책 | 연결되는 구현 |
|---|---|---|
| 관계망 조회 기능 | [SocialNetworkQueryUseCase](src/main/java/com/example/DunbarHorizon/social/application/port/in/SocialNetworkQueryUseCase.java) | [SocialNetworkQueryService](src/main/java/com/example/DunbarHorizon/social/application/service/SocialNetworkQueryService.java) |
| 관계망 조회 저장소 | [SocialNetworkRepository](src/main/java/com/example/DunbarHorizon/social/application/port/out/SocialNetworkRepository.java) | [SocialNetworkRepositoryAdapter](src/main/java/com/example/DunbarHorizon/social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java) — Cypher와 결과 매핑 |
| 계정 도메인 저장소 | [UserRepository](src/main/java/com/example/DunbarHorizon/account/domain/repository/UserRepository.java) | [UserRepositoryAdapter](src/main/java/com/example/DunbarHorizon/account/adapter/out/persistence/UserRepositoryAdapter.java) → Spring Data JPA |
| 네트워크 캐시 무효화 | [SocialNetworkCacheManager](src/main/java/com/example/DunbarHorizon/social/application/port/out/SocialNetworkCacheManager.java) | [SocialNetworkCacheAdapter](src/main/java/com/example/DunbarHorizon/social/adapter/out/redis/SocialNetworkCacheAdapter.java) → Redis 키 삭제 |
| Flag가 필요로 하는 친구 여부 | [FriendshipChecker](src/main/java/com/example/DunbarHorizon/flag/domain/flag/FriendshipChecker.java) | [FlagUserAdapter](src/main/java/com/example/DunbarHorizon/flag/adapter/out/user/FlagUserAdapter.java) → Social의 입력 포트 |

Controller는 UseCase를 호출하고, 서비스는 저장소·외부 기능의 인터페이스를 사용합니다. DB나 다른 도메인을 아는 구체 클래스는 출력 어댑터이며, Spring이 인터페이스에 구현체를 주입합니다.

출력 포트가 모두 `application/port/out`에 있는 것은 아닙니다. 도메인이 사용하는 저장소는 `domain/**/repository`, 도메인 협력 인터페이스는 해당 도메인에 있습니다. 또한 현재 [User](src/main/java/com/example/DunbarHorizon/account/domain/User.java)와 [SocialUser](src/main/java/com/example/DunbarHorizon/social/domain/socialUser/SocialUser.java)는 각각 JPA·Neo4j 매핑 어노테이션을 함께 가지므로, 도메인 모델을 영속성 모델과 완전히 분리한 구조는 아닙니다.

### 도메인과 저장소의 책임

| 패키지 | 담당 기능 | 저장소 |
|---|---|---|
| [account](src/main/java/com/example/DunbarHorizon/account) | 가입·인증, 프로필, 사용자 변경 Outbox | MySQL; 가입 대기 토큰은 Redis |
| [social](src/main/java/com/example/DunbarHorizon/social) | 친구·친구 요청·라벨, 관계망, 관심도·친밀도 | Neo4j; 조회 캐시는 Redis, 해제 관계 이력은 MySQL |
| [buzz](src/main/java/com/example/DunbarHorizon/buzz) | 수신 범위를 정한 콘텐츠 공유와 답글 | MongoDB |
| [flag](src/main/java/com/example/DunbarHorizon/flag) | 활동 모집·참여·초대, 활동 이후 회포 | MySQL |
| [trace](src/main/java/com/example/DunbarHorizon/trace) | 프로필 방문과 상호 방문 판정 | MySQL |
| [notification](src/main/java/com/example/DunbarHorizon/notification) | 기기 토큰, 알림 이력, Firebase FCM 발송 | 토큰은 MySQL, 이력은 MongoDB |

기본 `@Transactional`은 [JpaConfig](src/main/java/com/example/DunbarHorizon/global/config/database/JpaConfig.java)의 MySQL 트랜잭션 매니저를 사용합니다. Neo4j 작업은 [@Neo4jTransactional](src/main/java/com/example/DunbarHorizon/global/annotation/Neo4jTransactional.java)로 별도 매니저를 지정합니다. 여러 DB의 변경이 하나의 원자적 트랜잭션이 되는 것은 아닙니다.

## 요청 흐름 따라가기

인증된 요청에서는 [JwtAuthenticationFilter](src/main/java/com/example/DunbarHorizon/global/security/JwtAuthenticationFilter.java)가 쿠키의 JWT를 검증해 `SecurityContext`에 인증 정보를 넣습니다. [CurrentUserIdArgumentResolver](src/main/java/com/example/DunbarHorizon/global/annotation/CurrentUserIdArgumentResolver.java)가 그 사용자 ID를 Controller의 `@CurrentUserId` 인자로 전달합니다. 예외의 HTTP 응답 변환은 [GlobalExceptionHandler](src/main/java/com/example/DunbarHorizon/global/exception/GlobalExceptionHandler.java)에서 확인할 수 있습니다.

### 1. 읽기: 내 관계망 조회

```text
GET /api/v1/network?circleSize=DUNBAR
  → SocialNetworkController.getFriendsNetwork()
  → SocialNetworkQueryUseCase
  → SocialNetworkQueryService
  → SocialNetworkRepository
  → SocialNetworkRepositoryAdapter의 @Cacheable
      ├─ Redis 적중 → 저장된 List<NodeGraphResult> 반환
      └─ 미적중 → Neo4jClient로 Cypher 실행 → 결과 매핑·캐시 저장 → 반환
```

시작점은 [SocialNetworkController](src/main/java/com/example/DunbarHorizon/social/adapter/in/web/SocialNetworkController.java)입니다. 서비스는 [DunbarCircle](src/main/java/com/example/DunbarHorizon/social/domain/friend/DunbarCircle.java)의 5·15·50·150명 경계와 가지치기 인자를 조회 포트로 전달합니다. 어댑터의 Cypher는 친밀도순으로 친구를 먼저 제한하고, 그 경계 안의 연결을 조회해 결과 DTO로 매핑합니다.

기본·라벨 조회의 서비스 메서드는 Neo4j 트랜잭션을 열지 않고, 실제 조회 어댑터에 캐시와 읽기 트랜잭션을 둡니다. [RedisConfig](src/main/java/com/example/DunbarHorizon/global/config/RedisConfig.java)는 캐시 TTL 10분, `List<NodeGraphResult>` 직렬화, Redis 오류 시 조회를 계속하는 정책을 설정합니다. 친구 생성·삭제나 라벨 멤버 변경 후에는 [SocialNetworkCacheEvictListener](src/main/java/com/example/DunbarHorizon/social/application/eventListener/SocialNetworkCacheEvictListener.java)가 커밋 이후 캐시 무효화 포트를 호출합니다.

별도의 `/api/v1/network/edges` 흐름에서는 [SocialNetworkExposurePolicy](src/main/java/com/example/DunbarHorizon/social/domain/friend/SocialNetworkExposurePolicy.java)가 친구·2-hop 대상별 연결 노출 수를 결정합니다. 기본 관계망의 가지치기와 이 API의 노출 정책은 서로 다른 코드 경로입니다.

### 2. 쓰기와 도메인 간 조회: 활동 참여

`POST /api/v1/flags/{flagId}/participants`는 다음 순서로 처리합니다.

1. [FlagController](src/main/java/com/example/DunbarHorizon/flag/adapter/in/web/FlagController.java)가 [FlagParticipationUseCase](src/main/java/com/example/DunbarHorizon/flag/application/port/in/FlagParticipationUseCase.java)를 호출합니다.
2. [FlagParticipationService](src/main/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationService.java)가 MySQL 트랜잭션 안에서 [FlagParticipationManager](src/main/java/com/example/DunbarHorizon/flag/domain/flag/FlagParticipationManager.java)에 참여 판단을 맡깁니다.
3. Manager는 `FriendshipChecker`로 호스트와의 친구 여부를 확인합니다. `FlagUserAdapter`가 이 요청을 Social의 [FriendshipQueryUseCase](src/main/java/com/example/DunbarHorizon/social/application/port/in/FriendshipQueryUseCase.java)에 연결합니다. Flag 도메인 코드는 Neo4j 저장소를 직접 호출하지 않습니다.
4. Manager는 Flag를 비관적 잠금으로 읽고 중복 참여와 현재 인원을 확인합니다. [Flag.participate()](src/main/java/com/example/DunbarHorizon/flag/domain/flag/Flag.java)가 호스트 참여 금지·모집 마감·정원 규칙을 검사하고 참여 객체를 만듭니다.
5. 서비스가 도메인의 [FlagRepository](src/main/java/com/example/DunbarHorizon/flag/domain/flag/repository/FlagRepository.java)를 통해 참여자를 저장합니다. [FlagRepositoryAdapter](src/main/java/com/example/DunbarHorizon/flag/adapter/out/persistence/FlagRepositoryAdapter.java)가 JPA에 위임하고, 커밋 후 Controller가 `201`을 반환합니다.

이처럼 다른 도메인의 정보가 즉시 필요할 때는 **소비하는 도메인의 포트 → 연결 어댑터 → 제공하는 도메인의 입력 포트**를 따라갑니다. 상태 변경을 알리는 경우에는 다음의 이벤트 경로를 사용합니다.

## 이벤트 흐름: Account → Social 사용자 동기화

계정 정보의 원천은 MySQL의 Account이며, Neo4j의 Social은 관계 조회에 필요한 사용자 정보를 복제해서 가집니다. 두 저장소의 반영 시점을 분리하고, MySQL Outbox에 미완료 작업을 남겨 재발행합니다. 메시지 브로커가 아니라 같은 애플리케이션의 Spring 이벤트를 사용합니다.

```text
Account의 MySQL 트랜잭션
  사용자 저장·변경 → 사용자 이벤트 발행
  → UserOutboxEventListener [BEFORE_COMMIT]
      PENDING Outbox 저장 + UserSyncIntegrationEvent 발행
  → 사용자 변경과 Outbox를 함께 커밋

SocialUserEventListener [AFTER_COMMIT, @Async]
  → SocialUserSyncCommandService.sync() [Neo4j REQUIRES_NEW]
      SocialUser 생성·상태 변경·프로필 갱신
  → 서비스 프록시 반환: Neo4j 커밋 성공
  → UserSyncCompletedEvent 발행
  → UserOutboxEventListener.onSyncCompleted() [MySQL REQUIRES_NEW]
      Outbox를 COMPLETED로 변경하고 커밋
```

코드를 읽을 때의 연결 지점은 다음과 같습니다.

- 발행: [SignupService](src/main/java/com/example/DunbarHorizon/account/application/service/SignupService.java)는 사용자 저장 후 활성화 이벤트를 직접 발행합니다. 프로필 변경·비활성화는 `User`가 이벤트를 등록하고, [BaseTimeAggregateRoot](src/main/java/com/example/DunbarHorizon/global/common/BaseTimeAggregateRoot.java)의 `@DomainEvents`를 통해 저장 시 발행합니다.
- 기록과 완료: [UserOutboxEventListener](src/main/java/com/example/DunbarHorizon/account/application/eventListener/UserOutboxEventListener.java)는 MySQL Outbox 기록과 완료 처리를 담당합니다. [UserEventOutbox](src/main/java/com/example/DunbarHorizon/account/domain/outbox/UserEventOutbox.java)에 상태·재시도 횟수·처리 시각을 보관합니다.
- 수신과 커밋: [SocialUserEventListener](src/main/java/com/example/DunbarHorizon/social/application/eventListener/SocialUserEventListener.java)는 비동기 작업을 조율하고, 별도 빈인 [SocialUserSyncCommandService](src/main/java/com/example/DunbarHorizon/social/application/service/SocialUserSyncCommandService.java)가 그래프 트랜잭션을 담당합니다. **Neo4j 저장 메서드가 실행된 시점이 아니라 트랜잭션 프록시가 성공 반환한 뒤에 완료 이벤트를 발행**합니다. 이 완료 이벤트의 MySQL 처리는 같은 비동기 작업 스레드에서 동기적으로 호출됩니다.
- 재발행: [UserOutboxRetryScheduler](src/main/java/com/example/DunbarHorizon/account/adapter/in/scheduler/UserOutboxRetryScheduler.java)가 5분 간격으로 [UserOutboxRetryService](src/main/java/com/example/DunbarHorizon/account/application/service/UserOutboxRetryService.java)를 호출합니다. 생성 후 5분이 지난 `PENDING` 기록을 재발행하고, 재시도 횟수가 5회에 도달한 기록은 다음 스캔에서 `FAILED`로 전환합니다.

Neo4j 처리·커밋 실패 시에는 완료 이벤트를 발행하지 않습니다. Neo4j 커밋 후 완료 이벤트가 전달되지 않거나 MySQL 완료 처리가 롤백되면 `PENDING`이 남으므로 같은 작업이 다시 전달될 수 있습니다. 이를 위해 활성화는 같은 사용자 ID를 재사용하고, 프로필 갱신은 `occurredAt`이 더 최신일 때만 적용합니다. 이것이 모든 이벤트의 순서나 정확히 한 번 실행을 보장한다는 뜻은 아닙니다.

이 Outbox는 사용자 복제 경로에 적용됩니다. `global/event`의 모든 이벤트가 영속화되거나 재시도되는 것은 아닙니다.

## 테스트

JDK 21을 설치하고 `JAVA_HOME` 또는 `PATH`에서 해당 Java를 찾을 수 있도록 설정합니다. Gradle은 저장소의 Wrapper를 사용합니다.
테스트는 JUnit 5·Mockito·AssertJ·Testcontainers를 사용하며, `src/test/java`에서 해당 도메인의 테스트를 찾을 수 있습니다.

| 검증하려는 책임 | 읽을 테스트·지원 코드 |
|---|---|
| 도메인의 판단 규칙 | [SocialNetworkExposurePolicyTest](src/test/java/com/example/DunbarHorizon/social/domain/friend/SocialNetworkExposurePolicyTest.java) |
| 유스케이스의 호출·저장 조율 | [FlagParticipationServiceTest](src/test/java/com/example/DunbarHorizon/flag/application/service/flag/FlagParticipationServiceTest.java) — 의존성을 모킹한 단위 테스트 |
| 사용자 동기화 로직·완료 이벤트 조율 | [SocialUserSyncCommandServiceTest](src/test/java/com/example/DunbarHorizon/social/application/service/SocialUserSyncCommandServiceTest.java), [SocialUserEventListenerTest](src/test/java/com/example/DunbarHorizon/social/application/eventListener/SocialUserEventListenerTest.java) — Mockito 단위 테스트 |
| MySQL·Neo4j 커밋 경계와 이벤트 유실 | [UserSyncCommitBoundaryIntegrationTest](src/test/java/com/example/DunbarHorizon/account/application/eventListener/UserSyncCommitBoundaryIntegrationTest.java) — 실제 DB·Spring 프록시와 테스트 전용 장애 주입. 아래 검증 상태 참조 |
| 실제 Cypher·JPA 저장소 | [SocialNetworkRepositoryAdapterTest](src/test/java/com/example/DunbarHorizon/social/adapter/out/SocialNetworkRepositoryAdapterTest.java), [UserEventOutboxRepositoryTest](src/test/java/com/example/DunbarHorizon/account/adapter/out/persistence/UserEventOutboxRepositoryTest.java) |
| Controller·보안 인자·응답 | [BaseControllerTest](src/test/java/com/example/DunbarHorizon/support/BaseControllerTest.java)와 도메인별 `adapter/in/web` 테스트 |

DB 슬라이스는 `support`의 `@JpaRepositoryTest`, `@Neo4jRepositoryTest`, `@MongoRepositoryTest`를 사용합니다. 저장소 단독 테스트나 리스너를 직접 호출하는 Mockito 테스트는 Spring 이벤트 전달·비동기 실행·여러 DB의 커밋 순서까지 검증하지는 않습니다.

Outbox 통합 테스트는 [BaseUserSyncIntegrationTest](src/test/java/com/example/DunbarHorizon/support/BaseUserSyncIntegrationTest.java)를 상속합니다. AFTER_COMMIT이 실제로 실행돼야 하므로 테스트 전체를 자동 롤백하는 대신 원본 MySQL 트랜잭션을 직접 커밋합니다. [테스트 구성](src/test/java/com/example/DunbarHorizon/support/UserSyncIntegrationTestConfig.java)은 공용 Testcontainers와 실제 저장소·리스너만 연결하며, OAuth·Firebase·스케줄러는 로드하지 않습니다.

[UserSyncFaultProbe](src/test/java/com/example/DunbarHorizon/support/UserSyncFaultProbe.java)는 테스트에서만 커밋 직전 대기·예외와 이벤트 전달 누락을 주입합니다. 작성된 시나리오는 다음 네 가지입니다.

- Neo4j 커밋 전에는 `PENDING`, 커밋 성공 후에는 `COMPLETED`인지 확인
- Neo4j 커밋 직전 예외 → 그래프 롤백·`PENDING` 유지 → 실제 재시도로 복구
- 최초 동기화 이벤트 전달 누락 → 원본과 Outbox 보존 → 재시도로 복구
- 완료 이벤트 전달 누락 → 그래프는 존재·Outbox는 `PENDING` → 재시도 후 노드 중복 없이 완료

이는 서버의 COMMIT 응답 실패·유실, 네트워크 단절 또는 프로세스 종료 자체를 재현하는 테스트는 아닙니다. 단위 테스트는 통과했으며, 이 네 통합 시나리오의 실제 DB 검증은 아래 상태처럼 아직 대기 중입니다.

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

Docker가 실행 중이어야 합니다. [공용 Testcontainers 설정](src/test/java/com/example/DunbarHorizon/support/TestContainerConfig.java)은 MySQL 8.0, Neo4j 5.24(APOC), MongoDB 7.0을 시작합니다. S3 어댑터 테스트는 별도의 LocalStack 컨테이너를 사용합니다. 테스트 데이터는 운영 DB와 분리합니다.

```bash
# MySQL Outbox 저장소와 Neo4j 연결 중개인 쿼리
./gradlew test --tests '*UserEventOutboxRepositoryTest' --tests '*SocialConnectionPathRepositoryAdapterTest'

# 사용자 동기화의 커밋 경계·이벤트 유실·재시도 (외부 서비스 자격증명 불필요)
./gradlew test --tests '*UserSyncCommitBoundaryIntegrationTest'

# 전체 테스트
./gradlew test

# 테스트를 포함한 빌드
./gradlew build
```

테스트 기본값은 [application-test.yml](src/test/resources/application-test.yml)을 참고합니다. 전체 테스트에는 애플리케이션 컨텍스트를 올리는 테스트도 포함돼 있습니다. 현재 Firebase 초기화와 OAuth·메일 설정이 완전히 격리되어 있지 않으므로, 아래 실행 설정 중 테스트에서 대체되지 않는 값과 Firebase 설정 파일이 필요합니다. **Docker만 설치하면 외부 연동 설정 없이 전체 테스트가 실행되는 구성은 아닙니다.**

테스트 리포트는 `build/reports/tests/test/index.html`에 생성됩니다.

## 애플리케이션 실행

MySQL·Neo4j(APOC)·MongoDB·Redis를 준비하고, [application.yml](src/main/resources/application.yml)에 맞춰 환경 변수를 주입합니다. 연결 정보와 자격증명은 저장소에 커밋하지 않습니다.

| 설정 | 내용 |
|---|---|
| MySQL | `MYSQL_URL`, `DB_USERNAME`, `MYSQL_PASSWORD`. URL·사용자명에는 로컬 기본값이 있습니다. |
| Neo4j | `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`. URI·사용자명에는 로컬 기본값이 있습니다. 현재 DB 이름도 `NEO4J_USERNAME` 설정을 사용합니다. |
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
- 2026-08-31 커밋 순서 수정 후 관련 단위 테스트 19개가 통과했습니다. 새 통합 테스트 4개는 컴파일됐으나 Docker 엔진 미연결로 테스트 컨텍스트 초기화가 실패하여, 실제 DB 검증은 아직 완료하지 못했습니다. 단위 테스트 통과가 이 검증을 대신하지는 않습니다.
- 현재 배포는 컨테이너 교체 중 짧은 중단을 허용합니다. `latest` 태그를 사용하며, 이전 버전으로의 복구 절차는 후속 과제입니다.

## 설계 배경과 작업 기록

- [기술 문서](https://valiant-vest-6c8.notion.site/DunbarHorizon-3776b857043480998e6fdebaba6ba662): 관계망 모델, 쿼리 실행 계획과 캐시 성능 실험, 대안 비교. 성능 실험은 k6 합성 부하이며 조건과 측정 근거를 함께 기록했습니다.
- 연결 중개인 조회의 범위·응답 결정: [task-105](harness/tasks/task-105-connection-path-scope.md) → [구현 커밋](https://github.com/Dcaffein/dunbar-horizon-backend/commit/a2f869c2f31f29675f1db03b0ea88de863b7ba65) → [상한·정렬·노출 정책 통합 테스트](src/test/java/com/example/DunbarHorizon/social/adapter/out/SocialConnectionPathRepositoryAdapterTest.java).
- AI 에이전트와의 작업 규칙: [작업 절차](harness/WORKFLOW.md), [아키텍처 규칙](harness/ARCHITECTURE.md), [테스트 규칙](harness/TESTING-GUIDE.md), [발주 문서 규칙](harness/TASK-SPEC-GUIDE.md). 제품 목적·제약을 태스크에 남기고, 코드 탐색을 거친 계획을 검토하는 방식입니다.
