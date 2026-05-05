# PLAN — Task 19: Social 도메인 동시성 통합 테스트 추가

## 작업 목표

`FriendRequesterActionService.sendRequest()`에 두 트랜잭션이 동시에 접근할 때
`FriendRequest.pairKey` DB 유니크 제약이 실제로 방어하는지 검증하는 통합 테스트를 추가한다.

---

## 생성 파일

| 구분 | 파일 |
|------|------|
| 신규 생성 | `src/test/java/com/example/DunbarHorizon/social/adapter/out/neo4j/FriendRequestConcurrencyTest.java` |

---

## 테스트 환경 구성

`Neo4jRepositoryTest`는 `@DataNeo4jTest` + `@Transactional` 조합이어서 두 가지 이유로 사용 불가:
1. 서비스 빈(`FriendRequesterActionService`)이 로드되지 않음
2. `@Transactional`이 붙으면 멀티스레드 각각이 독립 트랜잭션을 가질 수 없음

따라서 아래 어노테이션을 직접 조합한다:

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FriendRequestConcurrencyTest { ... }
```

**`@Transactional`은 테스트 클래스와 메서드 양쪽 모두 금지.**
데이터 정리는 `@AfterEach`에서 `FriendRequestNeo4jRepository.deleteAll()`과
`SocialUserNeo4jRepository.deleteById()`로 수동 처리한다.

---

## 구현 상세

### 주입 대상

```java
@Autowired FriendRequesterActionService friendRequesterActionService;
@Autowired FriendRequestNeo4jRepository friendRequestNeo4jRepository;
@Autowired SocialUserNeo4jRepository    socialUserNeo4jRepository;
```

### @BeforeEach / @AfterEach

```java
@BeforeEach
void setUp() {
    userA = socialUserNeo4jRepository.save(new SocialUser(1L, "userA", ""));
    userB = socialUserNeo4jRepository.save(new SocialUser(2L, "userB", ""));
}

@AfterEach
void tearDown() {
    friendRequestNeo4jRepository.deleteAll();
    socialUserNeo4jRepository.deleteById(userA.getId());
    socialUserNeo4jRepository.deleteById(userB.getId());
}
```

### 검증 시나리오

**시나리오 1 — 동일 방향 동시 요청 (A→B 두 번)**

```java
// 두 스레드가 동시에 sendRequest(A.id, B.id) 호출
// 기대: successCount == 1, failCount == 1 (DuplicateFriendRequestException)
```

**시나리오 2 — 역방향 동시 요청 (A→B / B→A)**

```java
// 스레드1: sendRequest(A.id, B.id)
// 스레드2: sendRequest(B.id, A.id)
// pairKey는 방향 무관 동일("1_2") → 기대: successCount == 1, failCount == 1
```

### 공통 패턴

```java
CountDownLatch startLatch = new CountDownLatch(1);
CountDownLatch doneLatch  = new CountDownLatch(2);
AtomicInteger successCount = new AtomicInteger(0);
AtomicInteger failCount    = new AtomicInteger(0);

// 각 스레드 Runnable 구성 후
startLatch.countDown();          // 동시 출발
doneLatch.await(5, TimeUnit.SECONDS);

assertThat(successCount.get()).isEqualTo(1);
assertThat(failCount.get()).isEqualTo(1);
```

---

## 예상 사이드 이펙트

- 없음. 신규 테스트 파일만 추가하며 프로덕션 코드 변경 없음.
- `@SpringBootTest`는 전체 컨텍스트를 로드하므로 테스트 실행 시간이 길 수 있음.
