package com.example.DunbarHorizon.social.application.friendship;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.InteractionScores;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendshipRepository;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 모임 종료 점수는 flag의 JPA 트랜잭션이 커밋된 직후 콜백(AFTER_COMMIT) 안에서 새로운 MySQL 트랜잭션을 연다.
 * 그 자리에서 새 MySQL 트랜잭션이 실제로 열려 커밋되는지, 잠금부터 쓰기까지 하나로 묶이는지 본다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagConclusionScoreAfterCommitIntegrationTest {

    private static final Long HOST_ID = 9101L;
    private static final List<Long> PARTICIPANT_IDS = List.of(9102L, 9103L);
    private static final List<Long> MEMBER_IDS = List.of(9101L, 9102L, 9103L);
    private static final InteractionType TYPE = InteractionType.FLAG_ENDED;
    private static final double DELTA = InteractionScores.scoreOf(TYPE);
    private static final double TOLERANCE = 0.000001;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private JdbcTemplate jdbc;

    private final List<Friendship> friendships = new ArrayList<>();

    @BeforeEach
    void setUp() {
        deleteMembers();
        for (long memberId : MEMBER_IDS) {
            jdbc.update("INSERT INTO users (user_id, email, nickname, role, status) VALUES (?, ?, ?, 'USER', 'ACTIVE')",
                    memberId, memberId + "@flag-conclusion.test", "사용자" + memberId);
        }
        friendships.clear();
        for (int i = 0; i < MEMBER_IDS.size(); i++) {
            for (int j = i + 1; j < MEMBER_IDS.size(); j++) {
                friendships.add(FriendTestFactory.createFriendship(MEMBER_IDS.get(i), MEMBER_IDS.get(j)));
            }
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendships.forEach(friendshipRepository::create));
    }

    @AfterEach
    void tearDown() {
        deleteMembers();
    }

    @Test
    @DisplayName("JPA 트랜잭션이 커밋된 뒤 모임의 모든 쌍에 점수가 저장된다")
    void batchEvent_SavesEveryPair_AfterJpaCommit() {
        // when
        publishInJpaTransaction(1L);

        // then
        double expected = FriendRecognition.INITIAL_RAW_SCORE + DELTA;
        for (Friendship created : friendships) {
            assertBothSides(created, expected);
        }
    }

    @Test
    @DisplayName("이 경로로 같은 모임 점수를 동시에 몰아넣어도 한 건도 잃지 않는다")
    void batchEvent_KeepsEveryUpdate_Concurrent() throws Exception {
        // given
        int events = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (long flagId = 1; flagId <= events; flagId++) {
                long currentFlagId = flagId;
                futures.add(executor.submit(() -> {
                    start.await();
                    publishInJpaTransaction(currentFlagId);
                    return null;
                }));
            }

            // when
            start.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException exception) {
                    throw new AssertionError("이벤트 처리가 실패했다", exception.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        // then
        double expected = FriendRecognition.INITIAL_RAW_SCORE + events * DELTA;
        for (Friendship created : friendships) {
            assertBothSides(created, expected);
        }
    }

    private void publishInJpaTransaction(long flagId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(new BatchMutualInteractionEvent(
                        flagId, PARTICIPANT_IDS, HOST_ID, TYPE, ScoreDirection.GRANT)));
    }

    private void assertBothSides(Friendship created, double expected) {
        Friendship friendship = friendshipRepository.findBetween(created.getLower().getOwnerId(),
                created.getHigher().getOwnerId()).orElseThrow();
        for (Long memberId : MEMBER_IDS) {
            if (friendship.getUserIds().contains(memberId)) {
                assertThat(friendship.getMyInterestScore(memberId))
                        .as("%s의 %d 쪽 점수", friendship.getUserIds(), memberId)
                        .isCloseTo(expected, within(TOLERANCE));
            }
        }
    }

    private void deleteMembers() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendships.forEach(created -> friendshipRepository.deleteBetween(
                        created.getLower().getOwnerId(), created.getHigher().getOwnerId())));
        for (long memberId : MEMBER_IDS) {
            jdbc.update("DELETE FROM users WHERE user_id = ?", memberId);
        }
    }
}
