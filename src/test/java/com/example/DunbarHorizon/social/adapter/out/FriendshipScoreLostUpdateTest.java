package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.application.service.FriendshipDecayService;
import com.example.DunbarHorizon.social.application.service.InteractionScoreService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 같은 관계에 점수 갱신을 몰아넣었을 때 한 건도 잃지 않는지 본다.
 * <p>
 * block 포맷에서는 잠금과 같은 쿼리 안의 읽기가 잠금 대기 전 값을 보아 갱신이 조용히 사라진다.
 * aligned에서는 이 유실이 나지 않으므로, 포맷이 block이 아니면 초록불이 근거가 되지 못해 먼저 실패시킨다.
 * <p>
 * 실제 커밋과 잠금 경합이 필요하므로 테스트 트랜잭션을 끈다.
 */
@Neo4jRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipRepositoryAdapter.class, InteractionScoreService.class,
        FriendshipDecayService.class, FriendshipDecayPolicy.class})
class FriendshipScoreLostUpdateTest {

    private static final long FIRST_USER_ID = 1L;
    private static final long LAST_USER_ID = 4L;
    private static final int THREADS = 16;
    private static final int CALLS_PER_FRIENDSHIP = 50;
    private static final int FLAG_CONCLUSIONS = 30;
    private static final int CONCURRENT_DECAYS = 10;
    private static final double TOLERANCE = 0.000001;

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private InteractionScoreService interactionScoreService;

    @Autowired
    private FriendshipDecayService friendshipDecayService;

    @Autowired
    private FriendshipDecayPolicy friendshipDecayPolicy;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private List<Friendship> friendships;

    @BeforeEach
    void setUp() {
        assertThat(storeFormat())
                .as("block 포맷이 아니면 유실이 재현되지 않아 이 테스트의 통과가 근거가 되지 못한다")
                .startsWith("block");

        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        List<SocialUser> users = LongStream.rangeClosed(FIRST_USER_ID, LAST_USER_ID)
                .mapToObj(id -> socialUserNeo4jRepository.save(new SocialUser(id, "사용자" + id, null)))
                .toList();

        friendships = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                Friendship friendship = FriendTestFactory.createFriendship(users.get(i), users.get(j));
                friendshipRepository.create(friendship);
                friendships.add(friendship);
            }
        }
    }

    @AfterEach
    void tearDown() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    @DisplayName("같은 관계에 상호 갱신을 동시에 몰아넣어도 한 건도 잃지 않는다")
    void applyInteraction_KeepsEveryUpdate_ConcurrentMutual() {
        // given
        InteractionType type = InteractionType.FLAG_ENDED;
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            for (int i = 0; i < CALLS_PER_FRIENDSHIP; i++) {
                tasks.add(() -> {
                    interactionScoreService.applyInteraction(pair[0], pair[1], type);
                    return null;
                });
            }
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("실패들=%s", failures).isEmpty();
        double expected = FriendRecognition.INITIAL_RAW_SCORE + CALLS_PER_FRIENDSHIP * delta;
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            assertThat(interestScoreOf(friendship.getId(), pair[0])).as(friendship.getId() + " low")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(interestScoreOf(friendship.getId(), pair[1])).as(friendship.getId() + " high")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(friendshipRepository.findById(friendship.getId()).orElseThrow().getIntimacy())
                    .as(friendship.getId() + " intimacy")
                    .isCloseTo(FriendRecognition.normalize(expected), within(TOLERANCE));
        }
    }

    @Test
    @DisplayName("같은 관계에 양쪽의 단방향 갱신을 동시에 몰아넣어도 한 건도 잃지 않는다")
    void applyInteraction_KeepsEveryUpdate_ConcurrentOneSided() {
        // given
        InteractionType type = InteractionType.BUZZ_SEND;
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            for (int i = 0; i < CALLS_PER_FRIENDSHIP; i++) {
                tasks.add(() -> {
                    interactionScoreService.applyInteraction(pair[0], pair[1], type);
                    return null;
                });
                tasks.add(() -> {
                    interactionScoreService.applyInteraction(pair[1], pair[0], type);
                    return null;
                });
            }
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("실패들=%s", failures).isEmpty();
        double expected = FriendRecognition.INITIAL_RAW_SCORE + CALLS_PER_FRIENDSHIP * delta;
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            assertThat(interestScoreOf(friendship.getId(), pair[0])).as(friendship.getId() + " low")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(interestScoreOf(friendship.getId(), pair[1])).as(friendship.getId() + " high")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(friendshipRepository.findById(friendship.getId()).orElseThrow().getIntimacy())
                    .as(friendship.getId() + " intimacy")
                    .isCloseTo(FriendRecognition.normalize(expected), within(TOLERANCE));
        }
    }

    @Test
    @DisplayName("같은 모임의 종료 점수를 동시에 몰아넣어도 모든 쌍에서 한 건도 잃지 않는다")
    void applyFlagConclusion_KeepsEveryUpdate_ConcurrentBatches() {
        // given
        InteractionType type = InteractionType.FLAG_ENDED;
        double delta = InteractionScorePolicy.scoreOf(type);
        List<Long> participantIds = List.of(2L, 3L, 4L);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (long flagId = 1; flagId <= FLAG_CONCLUSIONS; flagId++) {
            long currentFlagId = flagId;
            tasks.add(() -> {
                interactionScoreService.applyFlagConclusion(
                        currentFlagId, FIRST_USER_ID, participantIds, type, ScoreDirection.GRANT);
                return null;
            });
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("실패들=%s", failures).isEmpty();
        double expected = FriendRecognition.INITIAL_RAW_SCORE + FLAG_CONCLUSIONS * delta;
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            assertThat(interestScoreOf(friendship.getId(), pair[0])).as(friendship.getId() + " low")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(interestScoreOf(friendship.getId(), pair[1])).as(friendship.getId() + " high")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(friendshipRepository.findById(friendship.getId()).orElseThrow().getIntimacy())
                    .as(friendship.getId() + " intimacy")
                    .isCloseTo(FriendRecognition.normalize(expected), within(TOLERANCE));
        }
    }

    @Test
    @DisplayName("감쇄를 동시에 여러 번 돌려도 한 번도 잃지 않고 모두 곱해진다")
    void processDecay_KeepsEveryDecay_Concurrent() {
        // given
        double initial = FriendRecognition.INITIAL_RAW_SCORE;
        resetToStaleInteractions(initial);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_DECAYS; i++) {
            tasks.add(() -> {
                friendshipDecayService.processDecay();
                return null;
            });
        }

        // when
        List<Throwable> failures = runConcurrently(tasks);

        // then
        assertThat(failures).as("실패들=%s", failures).isEmpty();
        double expected = initial * Math.pow(friendshipDecayPolicy.getDecayRate(), CONCURRENT_DECAYS);
        for (Friendship friendship : friendships) {
            Long[] pair = pairOf(friendship);
            assertThat(interestScoreOf(friendship.getId(), pair[0])).as(friendship.getId() + " low")
                    .isCloseTo(expected, within(TOLERANCE));
            assertThat(interestScoreOf(friendship.getId(), pair[1])).as(friendship.getId() + " high")
                    .isCloseTo(expected, within(TOLERANCE));
        }
    }

    /**
     * 감쇄는 유예 기간이 지난 쪽만 건드리므로 교류 시각을 과거로 밀고 점수를 처음으로 되돌린다.
     */
    private void resetToStaleInteractions(double score) {
        neo4jClient.query(
                        "MATCH ()-[r:FRIENDS_WITH]->() " +
                        "SET r.lowInterestScore = $score, r.highInterestScore = $score, " +
                        "r.lowLastInteractedAt = localdatetime() - duration({days: 60}), " +
                        "r.highLastInteractedAt = localdatetime() - duration({days: 60})")
                .bind(score).to("score")
                .run();
    }

    private String storeFormat() {
        return neo4jClient.query("SHOW DATABASES YIELD name, store WHERE name = 'neo4j' RETURN store")
                .in("system")
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("store").asString())
                .one()
                .orElseThrow();
    }

    private static Long[] pairOf(Friendship friendship) {
        String[] ids = friendship.getId().split("_");
        return new Long[]{Long.parseLong(ids[0]), Long.parseLong(ids[1])};
    }

    private List<Throwable> runConcurrently(List<Callable<Void>> tasks) {
        List<Callable<Void>> shuffled = new ArrayList<>(tasks);
        Collections.shuffle(shuffled);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new ArrayList<>();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : shuffled) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
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

    private double interestScoreOf(String friendshipId, Long userId) {
        return neo4jClient.query(
                        "MATCH (low)-[r:FRIENDS_WITH {id: $friendshipId}]->() " +
                        "RETURN CASE WHEN low.id = $userId THEN r.lowInterestScore ELSE r.highInterestScore END AS score")
                .bind(friendshipId).to("friendshipId")
                .bind(userId).to("userId")
                .fetchAs(Double.class)
                .mappedBy((typeSystem, record) -> record.get("score").asDouble())
                .one()
                .orElseThrow();
    }
}
