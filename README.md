# DunbarHorizon

친구 관계를 네트워크 맵으로 보여주고, 그 위에서 소통하고 함께할 활동을 만드는 SNS의 백엔드입니다.
관심은 각자의 것으로, 친밀도는 두 사람의 관계로 모델링하고, 친밀도와 노출 정책에 따라 관계망의 탐색 범위를 정합니다.

2025년 9월부터 기획·도메인 설계·백엔드·프론트엔드·배포를 1인으로 진행하고 있습니다. 이 저장소는 Java / Spring Boot 기반 REST API를 담고 있습니다.

[서비스](https://www.dunbarhorizon.link) · [기술 문서 및 체험 안내](https://valiant-vest-6c8.notion.site/DunbarHorizon-3776b857043480998e6fdebaba6ba662) · [프론트엔드 저장소](https://github.com/Dcaffein/dunbar-horizon-frontend)

## 먼저 볼 만한 사례

### 1. 관계망 조회 — 탐색 범위와 실행 비용을 함께 줄이기

친구 관계를 전부 펼친 뒤 걸러내던 조회에 친밀도 기반 탐색 경계를 정했습니다. 실행 계획으로 탐색 연산과 메모리 사용을 비교하고, 동시 요청에서는 Redis 캐시를 적용했습니다. 캐시 적중 뒤에도 남은 병목은 조회 서비스의 트랜잭션이 일으키는 Neo4j 왕복에서 찾았습니다.

별도 성능 실험 환경의 **k6 200 VU 합성 부하**에서 평균 HTTP 응답시간은 **477.8ms → 5.9ms**, 처리량은 **260.5 → 2,393.7 req/s**로 바뀌었습니다. 같은 30명 사용자 풀을 반복 조회하고 Redis 캐시를 워밍업한 조건의 결과이며, 실서비스 트래픽이나 모든 조회 경로의 성능을 뜻하지 않습니다.

- [조회 서비스](src/main/java/com/example/DunbarHorizon/social/application/service/SocialNetworkQueryService.java) · [Neo4j 조회 구현](src/main/java/com/example/DunbarHorizon/social/adapter/out/persistence/neo4j/SocialNetworkRepositoryAdapter.java) · [Redis 캐시](src/main/java/com/example/DunbarHorizon/social/adapter/out/redis/SocialNetworkCacheAdapter.java)
- [실제 그래프를 이용한 조회 테스트](src/test/java/com/example/DunbarHorizon/social/adapter/out/SocialNetworkRepositoryAdapterTest.java)
- 실험 조건과 단계별 근거: [기술 문서의 「관계망 조회 성능 개선」](https://valiant-vest-6c8.notion.site/DunbarHorizon-3776b857043480998e6fdebaba6ba662)

### 2. Account–Social 동기화 — 저장소를 나눈 뒤의 책임과 재처리

사용자 정보의 원천은 MySQL의 Account이고, Neo4j의 Social은 관계 탐색에 필요한 정보만 가진 복제본입니다. 사용자 변경과 Outbox 기록을 같은 MySQL 트랜잭션에 저장하고, 별도 이벤트 처리와 재발행으로 Social에 반영합니다. 재시도 한도에 도달한 기록은 `FAILED`로 남깁니다.

관심도 누적처럼 지연과 일부 유실을 감수하는 경로와, 계정 복제처럼 누락이 기능 사용에 영향을 주는 경로를 구분했습니다. 수신 처리·재시도·저장소 테스트는 있으며, **두 DB의 커밋 순서와 장애 복구를 끝까지 확인하는 통합 테스트는 아직 보강할 부분**입니다.

- [Outbox 기록·완료 처리](src/main/java/com/example/DunbarHorizon/account/application/eventListener/UserOutboxEventListener.java) · [Social 수신 처리](src/main/java/com/example/DunbarHorizon/social/application/eventListener/SocialUserEventListener.java) · [재시도](src/main/java/com/example/DunbarHorizon/account/application/service/UserOutboxRetryService.java)
- [수신 처리 단위 테스트](src/test/java/com/example/DunbarHorizon/social/application/SocialUserEventListenerTest.java) · [재시도 단위 테스트](src/test/java/com/example/DunbarHorizon/account/application/service/UserOutboxRetryServiceTest.java) · [MySQL 저장소 테스트](src/test/java/com/example/DunbarHorizon/account/adapter/out/persistence/UserEventOutboxRepositoryTest.java)

### 3. AI 에이전트 활용 — 제품의 결정과 구현 작업을 나누기

에이전트가 코드에서 알 수 없는 배경·목적·확정된 제약을 발주 문서에 적고, 코드 탐색과 구현 계획을 받은 뒤 검토하는 방식으로 작업합니다. 공통 규칙은 [harness](harness)에 두고, 작업별 이유는 태스크와 커밋에 남깁니다.

예를 들어 연결 중개인 API의 명세에는 프로필 표시와 분석 도구라는 서로 다른 목적이 섞여 있었습니다. 프로필 표시용으로 목적을 확정하고, 중개인 상위 3명과 전체 수만 반환하며 제3자의 친밀도 점수는 숨기도록 정리했습니다. 그 결정은 쿼리·응답 모델과 통합 테스트로 이어집니다.

- [문제와 결정: task-105](harness/tasks/task-105-connection-path-scope.md) · [구현 커밋](https://github.com/Dcaffein/dunbar-horizon-backend/commit/a2f869c2f31f29675f1db03b0ea88de863b7ba65) · [상한·정렬·노출 정책 통합 테스트](src/test/java/com/example/DunbarHorizon/social/adapter/out/SocialConnectionPathRepositoryAdapterTest.java)
- [작업 절차](harness/WORKFLOW.md) · [아키텍처 규칙](harness/ARCHITECTURE.md) · [테스트 규칙](harness/TESTING-GUIDE.md) · [발주 문서 규칙](harness/TASK-SPEC-GUIDE.md)

## 구성

Java 21 · Spring Boot 3.4.0 · Spring Security · JPA · Spring Data Neo4j / MongoDB · Redis를 사용합니다.
테스트는 JUnit 5, Mockito, AssertJ, Testcontainers를 사용하며, 배포는 Docker와 GitHub Actions, AWS ECR·S3·CodeDeploy로 구성했습니다.

| 영역 | 담당 기능 | 주 저장소 |
|---|---|---|
| Account | 가입·인증, 프로필, 사용자 변경 전달 | MySQL, Redis |
| Social | 친구·친구 요청·라벨, 관계망 조회, 관심도·친밀도 | Neo4j, Redis |
| Buzz | 수신 범위를 정한 콘텐츠 공유와 답글 | MongoDB |
| Flag | 활동 모집·참여·초대, 활동 이후 회포 | MySQL |
| Trace | 프로필 방문과 상호 방문 판정 | MySQL |
| Notification | 기기 토큰, 알림 이력, Firebase FCM 발송 | MySQL, MongoDB |

도메인별로 포트와 어댑터를 두는 헥사고날 구조입니다. 도메인 간 조회는 필요한 쪽이 선언한 인터페이스로 연결하고, 변경 통보에는 이벤트를 사용합니다.

```text
src/main/java/com/example/DunbarHorizon/
├── account / social / buzz / flag / trace / notification
│   ├── domain/          모델·규칙·도메인이 사용하는 인터페이스
│   ├── application/     유스케이스·포트·이벤트 처리
│   └── adapter/         HTTP·스케줄러·저장소·외부 서비스
└── global/              보안·설정·공통 이벤트·예외 처리
```

## 테스트

JDK 21을 설치하고 `JAVA_HOME` 또는 `PATH`에서 해당 Java를 찾을 수 있도록 설정합니다. Gradle은 저장소의 Wrapper를 사용합니다.

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
- Outbox 커밋 경계의 장애 주입·재처리 통합 검증은 아직 완료하지 않았습니다. 기존 단위 테스트와 저장소 테스트가 해당 검증을 대신하지는 않습니다.
- 현재 배포는 컨테이너 교체 중 짧은 중단을 허용합니다. `latest` 태그를 사용하며, 이전 버전으로의 복구 절차는 후속 과제입니다.
