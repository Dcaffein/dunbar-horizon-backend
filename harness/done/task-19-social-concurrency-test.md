# Task 19: Social 도메인 동시성 통합 테스트 추가

## Objective

`FriendRequesterActionService.sendRequest()`에 두 트랜잭션이 동시에 접근할 때
DB 레벨 유니크 제약(`pairKey`)이 실제로 방어하는지 검증하는 통합 테스트를 추가한다.

단위 테스트(Mockito)는 트랜잭션 경쟁 상황을 재현할 수 없으므로, 실제 Neo4j 컨테이너를
사용하는 `@SpringBootTest` 기반 통합 테스트로 작성한다.

## Domain Change
[ ] 없음

## Target Files

### 신규 생성 (테스트)
- `src/test/java/com/example/DunbarHorizon/social/adapter/out/neo4j/FriendRequestConcurrencyTest.java`

## Requirements

### 테스트 환경 구성

`Neo4jRepositoryTest`는 `@DataNeo4jTest` + `@Transactional` 조합이라 서비스 빈 로드 및
멀티스레드 트랜잭션 분리가 불가능하다.
따라서 아래 어노테이션을 직접 조합하여 테스트 클래스를 구성한다.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
// @Transactional 금지 — 붙이면 모든 스레드가 같은 트랜잭션에 묶임
class FriendRequestConcurrencyTest { ... }
```

테스트 데이터(SocialUser)는 `@BeforeEach`에서 저장하고,
`@AfterEach`에서 Neo4j 관련 Repository를 통해 직접 삭제하여 격리한다.

### 검증 시나리오

**시나리오 1: 동일 페어에 대한 동시 친구 요청**

- A→B 요청 2개를 `CountDownLatch`로 동시에 출발시킨다.
- 1개는 성공, 1개는 `DuplicateFriendRequestException`이 발생해야 한다.

```
successCount == 1
failCount    == 1
```

**시나리오 2: A→B와 B→A 동시 요청 (역방향)**

- `pairKey`는 방향에 무관하게 동일(`min_max`)하므로 두 요청 중 하나는 제약에 걸린다.
- 1개 성공, 1개 `DuplicateFriendRequestException`.

```
successCount == 1
failCount    == 1
```

### 구현 패턴

```java
CountDownLatch startLatch = new CountDownLatch(1);
CountDownLatch doneLatch  = new CountDownLatch(2);
AtomicInteger successCount = new AtomicInteger(0);
AtomicInteger failCount    = new AtomicInteger(0);

Runnable task = () -> {
    try {
        startLatch.await();
        friendRequesterActionService.sendRequest(userA.getId(), userB.getId());
        successCount.incrementAndGet();
    } catch (DuplicateFriendRequestException e) {
        failCount.incrementAndGet();
    } finally {
        doneLatch.countDown();
    }
};

ExecutorService executor = Executors.newFixedThreadPool(2);
executor.submit(task);
executor.submit(task);
startLatch.countDown();
doneLatch.await(5, TimeUnit.SECONDS);
```

## Decisions

| 항목 | 결정값 | 비고 |
|------|--------|------|
| 테스트 베이스 | `@SpringBootTest` 직접 사용 | `Neo4jRepositoryTest`는 `@Transactional` 포함으로 멀티스레드 불가 |
| 테스트 메서드 트랜잭션 | 없음 (`@Transactional` 금지) | 각 스레드가 독립 트랜잭션을 갖도록 |
| 데이터 정리 | `@AfterEach`에서 Repository 직접 삭제 | 테스트 간 격리 보장 |
| 동시성 제어 | `CountDownLatch` + `ExecutorService` | 표준 Java 동시성 테스트 패턴 |
| 대기 타임아웃 | `doneLatch.await(5, TimeUnit.SECONDS)` | 무한 대기 방지 |

## Testing Strategy

- TESTING-PROTOCOL.md의 통합 테스트 규칙을 따르되, `@Transactional` 제외
- Given-When-Then 구조 유지
- `assertThat(successCount.get()).isEqualTo(1)` / `assertThat(failCount.get()).isEqualTo(1)` 로 결과 검증
