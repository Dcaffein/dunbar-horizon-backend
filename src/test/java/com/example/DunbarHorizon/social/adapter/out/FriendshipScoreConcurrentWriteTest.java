package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.application.service.FriendshipDecayService;
import com.example.DunbarHorizon.social.application.service.IntimacyScoreManager;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.TransientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 점수 쓰기 네 커맨드가 같은 락 순서 규약(Friendship 노드 선점 + friendshipId 오름차순)을 쓰므로
 * 동시에 돌려도 교착이 성립하지 않는다는 것을 반증하려는 테스트다.
 * <p>
 * 교착은 타이밍에 의존하므로 통과가 부재의 증명은 아니다. 그래서 먼저 규약을 어긴 쿼리로
 * 교착을 실제로 재현해(네거티브 컨트롤) 이 하네스가 교착을 잡을 수 있음을 보인 뒤,
 * 같은 하네스로 현행 커맨드들을 경합시킨다. 컨트롤이 터지지 않으면 뒤 케이스의 초록불은 근거가 아니다.
 * <p>
 * 실제 커밋과 락 경합이 필요하므로 테스트 트랜잭션을 끈다.
 */
@Neo4jRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipRepositoryAdapter.class, IntimacyScoreManager.class,
        FriendshipDecayService.class, FriendshipDecayPolicy.class})
class FriendshipScoreConcurrentWriteTest {

    private static final long FIRST_USER_ID = 1L;
    private static final long LAST_USER_ID = 4L;
    private static final double DELTA = 1.0;
    private static final long LOCK_HOLD_MILLIS = 500L;
    private static final int DEADLOCK_ROUNDS = 3;

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private IntimacyScoreManager intimacyScoreManager;

    @Autowired
    private FriendshipDecayService friendshipDecayService;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    /**
     * 규약을 어긴 쿼리. 현행 커맨드들과 락 종류·획득 순서는 같지만, 호출자가 준 순서대로 잠근다.
     * 전역 순서가 없으므로 반대 순서로 들어온 둘 사이에 보유-대기 사이클이 성립한다.
     */
    private static final String LOCK_TWO_FRIENDSHIPS_IN_GIVEN_ORDER =
            "MATCH (first:Friendship {id: $firstId}) " +
            "SET first._interactionScoreLock = true " +
            "REMOVE first._interactionScoreLock " +
            "WITH first " +
            "CALL apoc.util.sleep($holdMillis) " +
            "WITH first " +
            "MATCH (second:Friendship {id: $secondId}) " +
            "SET second._interactionScoreLock = true " +
            "REMOVE second._interactionScoreLock";

    private List<String> friendshipIds;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        List<SocialUser> users = LongStream.rangeClosed(FIRST_USER_ID, LAST_USER_ID)
                .mapToObj(id -> socialUserNeo4jRepository.save(new SocialUser(id, "사용자" + id, null)))
                .toList();

        friendshipIds = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                Friendship friendship = FriendTestFactory.createFriendship(users.get(i), users.get(j));
                friendshipRepository.create(friendship);
                friendshipIds.add(friendship.getId());
            }
        }
    }

    @AfterEach
    void tearDown() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    @DisplayName("규약을 어긴 순서로 두 Friendship을 잠그면 교착이 발생한다")
    void 규약을_어긴_순서로_두_Friendship을_잠그면_교착이_발생한다() {
        // given
        String friendshipAB = Friendship.generateCompositeId(1L, 2L);
        String friendshipCD = Friendship.generateCompositeId(3L, 4L);

        for (int round = 1; round <= DEADLOCK_ROUNDS; round++) {
            // when
            List<Throwable> failures = runConcurrently(List.of(
                    () -> lockInGivenOrder(friendshipAB, friendshipCD),
                    () -> lockInGivenOrder(friendshipCD, friendshipAB)
            ));

            // then
            assertThat(failures)
                    .as("round %d: 반대 순서로 잠근 두 트랜잭션 중 하나는 교착으로 실패해야 한다", round)
                    .isNotEmpty();
            assertThat(failures)
                    .as("round %d: 실패 사유가 교착이어야 한다. 실패들=%s", round, failures)
                    .allMatch(FriendshipScoreConcurrentWriteTest::isDeadlock);
        }
    }

    @Test
    @DisplayName("같은 쿼리를 규약대로 같은 순서로 잠그면 교착이 없다")
    void 같은_쿼리를_규약대로_같은_순서로_잠그면_교착이_없다() {
        // given
        String friendshipAB = Friendship.generateCompositeId(1L, 2L);
        String friendshipCD = Friendship.generateCompositeId(3L, 4L);

        // when
        List<Throwable> failures = runConcurrently(List.of(
                () -> lockInGivenOrder(friendshipAB, friendshipCD),
                () -> lockInGivenOrder(friendshipAB, friendshipCD)
        ));

        // then
        assertThat(failures)
                .as("앞 케이스와 쿼리·락 보유 시간이 같고 순서만 다르다. 교착의 원인이 순서임을 가른다. 실패들=%s", failures)
                .isEmpty();
    }

    @Test
    @DisplayName("같은 Friendship 집합을 서로 다른 순서로 동시에 증분해도 교착이 없다")
    void 같은_Friendship_집합을_서로_다른_순서로_동시에_증분해도_교착이_없다() {
        // given
        int writerCount = 4;
        List<Callable<Void>> writers = new ArrayList<>();
        for (int i = 0; i < writerCount; i++) {
            List<MutualInterestScoreUpdate> updates = shuffledUpdates();
            writers.add(() -> {
                friendshipRepository.incrementMutualInterestScoresBatch(updates);
                return null;
            });
        }

        // when
        List<Throwable> failures = runConcurrently(writers);

        // then
        assertThat(failures).as("교착 없이 모두 끝나야 한다. 실패들=%s", failures).isEmpty();
        for (String friendshipId : friendshipIds) {
            assertThat(interestScoresOf(friendshipId))
                    .as("%s의 양쪽 점수가 writer 수만큼 누적되어야 한다", friendshipId)
                    .allSatisfy(score -> assertThat(score)
                            .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + writerCount * DELTA, within(0.0001)));
        }
    }

    @Test
    @DisplayName("감쇄와 점수 증분을 동시에 실행해도 교착이 없다")
    void 감쇄와_점수_증분을_동시에_실행해도_교착이_없다() {
        // given
        LocalDateTime decayTime = LocalDateTime.now().plusSeconds(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(() -> {
            friendshipRepository.applyDecay(0.967, 1.0, decayTime);
            return null;
        });
        tasks.add(() -> {
            friendshipRepository.applyDecay(0.967, 1.0, decayTime);
            return null;
        });
        for (int i = 0; i < 2; i++) {
            List<MutualInterestScoreUpdate> updates = shuffledUpdates();
            tasks.add(() -> {
                friendshipRepository.incrementMutualInterestScoresBatch(updates);
                return null;
            });
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("교착 없이 모두 끝나야 한다. 실패들=%s", failures).isEmpty();
    }

    @Test
    @DisplayName("단건 증분과 배치 증분을 동시에 실행해도 교착이 없다")
    void 단건_증분과_배치_증분을_동시에_실행해도_교착이_없다() {
        // given
        String friendshipAB = Friendship.generateCompositeId(1L, 2L);
        List<MutualInterestScoreUpdate> updates = shuffledUpdates();

        List<Callable<Void>> tasks = List.of(
                () -> {
                    friendshipRepository.incrementMutualInterestScore(friendshipAB, 1L, 2L, DELTA);
                    return null;
                },
                () -> {
                    friendshipRepository.incrementInterestScore(friendshipAB, 1L, 2L, DELTA);
                    return null;
                },
                () -> {
                    friendshipRepository.incrementMutualInterestScoresBatch(updates);
                    return null;
                }
        );

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("교착 없이 모두 끝나야 한다. 실패들=%s", failures).isEmpty();
        assertThat(interestScoreOf(friendshipAB, 1L))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 3 * DELTA, within(0.0001));
        assertThat(interestScoreOf(friendshipAB, 2L))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 2 * DELTA, within(0.0001));
    }

    @Test
    @DisplayName("Flag 종료 점수가 호출이 끝난 시점에 모든 참가자 쌍에 반영되어 있다")
    void Flag_종료_점수가_호출이_끝난_시점에_모든_참가자_쌍에_반영되어_있다() {
        // given
        List<Long> participantIds = List.of(2L, 3L, 4L);

        // when
        intimacyScoreManager.applyFlagConclusion(77L, FIRST_USER_ID, participantIds, DELTA);

        // then
        for (String friendshipId : friendshipIds) {
            assertThat(interestScoresOf(friendshipId))
                    .as("%s의 양쪽 점수가 반영되어 있어야 한다", friendshipId)
                    .allSatisfy(score -> assertThat(score)
                            .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + DELTA, within(0.0001)));
        }
    }

    @Test
    @DisplayName("Flag 종료 점수와 감쇄를 동시에 실행해도 교착이 없다")
    void Flag_종료_점수와_감쇄를_동시에_실행해도_교착이_없다() {
        // given
        ageAllInteractions(60);
        List<Long> participantIds = List.of(2L, 3L, 4L);
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(() -> {
            friendshipDecayService.processDecay();
            return null;
        });
        for (long flagId = 1; flagId <= 3; flagId++) {
            long currentFlagId = flagId;
            tasks.add(() -> {
                intimacyScoreManager.applyFlagConclusion(currentFlagId, FIRST_USER_ID, participantIds, DELTA);
                return null;
            });
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures)
                .as("교착이 재시도에 삼켜지지 않고 올라온다. 실패들=%s", failures)
                .isEmpty();
    }

    private Void lockInGivenOrder(String firstId, String secondId) {
        neo4jClient.query(LOCK_TWO_FRIENDSHIPS_IN_GIVEN_ORDER)
                .bind(firstId).to("firstId")
                .bind(secondId).to("secondId")
                .bind(LOCK_HOLD_MILLIS).to("holdMillis")
                .run();
        return null;
    }

    /**
     * 감쇄는 유예 기간이 지난 관계만 건드린다. 갓 만든 픽스처로는 아무 행도 잠그지 않아
     * 경합이 만들어지지 않으므로 마지막 교류 시각을 과거로 밀어 둔다.
     */
    private void ageAllInteractions(int days) {
        neo4jClient.query(
                        "MATCH (:UserReference)-[r:HAS_FRIENDSHIP]->(:Friendship) " +
                        "SET r.lastInteractedAt = localdatetime() - duration({days: $days})")
                .bind(days).to("days")
                .run();
    }

    private List<MutualInterestScoreUpdate> shuffledUpdates() {
        List<MutualInterestScoreUpdate> updates = friendshipIds.stream()
                .map(friendshipId -> {
                    String[] userIds = friendshipId.split("_");
                    return new MutualInterestScoreUpdate(
                            friendshipId, Long.parseLong(userIds[0]), Long.parseLong(userIds[1]), DELTA);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(updates);
        return updates;
    }

    /**
     * 모든 작업을 같은 순간에 출발시키고, 올라온 실패를 삼키지 않고 모아 돌려준다.
     */
    private List<Throwable> runConcurrently(List<Callable<Void>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new ArrayList<>();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();

            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 실행이 중단되었다", exception);
        } finally {
            executor.shutdownNow();
        }
        return failures;
    }

    private static boolean isDeadlock(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransientException transientException
                    && transientException.code().contains("DeadlockDetected")) {
                return true;
            }
        }
        return false;
    }

    private List<Double> interestScoresOf(String friendshipId) {
        return neo4jClient.query(
                        "MATCH (:UserReference)-[r:HAS_FRIENDSHIP]->(:Friendship {id: $friendshipId}) " +
                        "RETURN r.interestScore AS score")
                .bind(friendshipId).to("friendshipId")
                .fetchAs(Double.class)
                .mappedBy((typeSystem, record) -> record.get("score").asDouble())
                .all()
                .stream()
                .toList();
    }

    private double interestScoreOf(String friendshipId, Long userId) {
        return neo4jClient.query(
                        "MATCH (:UserReference {id: $userId})-[r:HAS_FRIENDSHIP]->(:Friendship {id: $friendshipId}) " +
                        "RETURN r.interestScore AS score")
                .bind(friendshipId).to("friendshipId")
                .bind(userId).to("userId")
                .fetchAs(Double.class)
                .mappedBy((typeSystem, record) -> record.get("score").asDouble())
                .one()
                .orElseThrow();
    }
}
