# Backend Testing Protocol

이 프로젝트는 신뢰성 높은 검증을 위해 단위 테스트와 통합 테스트(TestContainers 기반)를 병행한다. 새로운 코드를 작성하면 반드시 규칙에 맞는 테스트를 작성해야 한다.
테스트 코드의 폴더 구조는 소스 코드와 일치시킨다.

## 1. 프레임워크 및 도구
- 프레임워크: JUnit 5
- 모킹 및 검증: Mockito, AssertJ
- 인프라 격리: TestContainers

## 2. 단위 테스트 (Unit Tests)
- **대상**: `domain` 로직 및 `application/service`.
- **규칙**: 외부 시스템이나 데이터베이스를 띄우지 않고, Mockito를 사용하여 순수 비즈니스 로직만 검증한다.

## 3. 통합 및 슬라이스 테스트 (Integration/Slice Tests)
테스트 환경을 수동으로 구성하지 말고, 프로젝트 패키지 `com.example.DunbarHorizon.support`에 미리 정의된 Base 클래스를 반드시 상속(extends)하여 작성하라.

- **Web / Controller 테스트**:
    - `BaseControllerTest`를 상속받아라.
    - 보안/인증이 필요한 API는 `@WithMockCustomUser` 어노테이션을 사용하여 인증된 상태를 모킹하라.
- **Persistence / Repository 테스트**:
    - JPA 관련 테스트: `JpaRepositoryTest`를 상속받아라.
    - Neo4j 관련 테스트: `Neo4jRepositoryTest`를 상속받아라.
    - MongoDB 관련 테스트: `MongoRepositoryTest`를 상속받아라.
    - `TestContainerConfig.java`를 통해 Docker 컨테이너가 띄워지므로 실제 DB와 동일한 환경에서 쿼리를 검증해야 한다.

## 4. 테스트 작성 규칙 (Given-When-Then)
- 테스트 케이스의 이름을 명확하게 작성하라 (예: `정상적인_친구_요청_시_상태가_PENDING으로_저장된다()`).
- 모든 테스트 코드는 `// given`, `// when`, `// then`의 3단계 주석으로 구분하여 가독성을 높여라.
- Happy Path(성공) 뿐만 아니라 도메인 내부 Exception(예: `DuplicateFriendRequestException` 등)이 발생하는 Edge Case를 꼼꼼히 검증하라.