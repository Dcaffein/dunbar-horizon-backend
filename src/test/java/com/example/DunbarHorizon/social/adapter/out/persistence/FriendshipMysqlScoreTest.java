package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.UserProfileJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.eventListener.LabelMembershipEventListener;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.FriendshipScoreService;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class, UserProfileJdbcRepository.class, LabelJdbcRepository.class,
        FriendshipScoreService.class, FriendshipCommandService.class, FriendshipDecayPolicy.class,
        LabelPersistenceAdapter.class, ActiveUserPersistenceAdapter.class, LabelCommandService.class,
        LabelNamer.class, LabelMemberRegistrar.class, LabelMembershipEventListener.class})
class FriendshipMysqlScoreTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime OLD_INTERACTION = CREATED_AT.atStartOfDay();
    private static final LocalDateTime RECENT_INTERACTION = LocalDateTime.now().withNano(0);

    @Autowired private UserJpaRepository users;
    @Autowired private FriendshipPersistenceAdapter friendships;
    @Autowired private FriendshipScoreService scores;
    @Autowired private FriendshipCommandService commands;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private Long low;
    private Long middle;
    private Long high;

    @BeforeEach
    void setUp() {
        low = users.save(User.createActive("low@mysql-scores.test", "low")).getId();
        middle = users.save(User.createActive("middle@mysql-scores.test", "middle")).getId();
        high = users.save(User.createActive("high@mysql-scores.test", "high")).getId();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM friendships WHERE low_user_id IN (?, ?, ?)", low, middle, high);
        users.deleteAllById(List.of(low, middle, high));
    }

    @Test
    @DisplayName("양쪽 단방향 상호작용과 상호 갱신을 기존 점수 규칙으로 적용한다")
    void applyInteraction_UpdatesOnlyActorForDirectionalEvents() {
        // given
        create(low, high, 21.4, 30, OLD_INTERACTION, OLD_INTERACTION);

        // when
        scores.applyInteraction(high, low, InteractionType.BUZZ_REPLY);

        // then
        Friendship first = assertScores(low, high, 21.4, 32);
        assertThat(first.getLower().getLastInteractedAt()).isEqualTo(OLD_INTERACTION);
        assertThat(first.getHigher().getLastInteractedAt()).isAfter(OLD_INTERACTION);
        scores.applyInteraction(low, high, InteractionType.VISIT);
        assertScores(low, high, 22.4, 32);
        scores.applyInteraction(high, low, InteractionType.FLAG_ENDED);
        assertScores(low, high, 32.4, 42);
    }

    @Test
    @DisplayName("Flag 종료 배치는 존재하는 모든 참여자 쌍만 함께 갱신하고 회수 점수는 0에서 멈춘다")
    void applyFlagConclusion_UpdatesExistingPairsAndClampsRevoke() {
        // given
        create(low, middle, 4, 8, OLD_INTERACTION, OLD_INTERACTION);
        create(middle, high, 4, 8, OLD_INTERACTION, OLD_INTERACTION);

        // when
        scores.applyFlagConclusion(low, List.of(high, middle), InteractionType.FLAG_ENDED, ScoreDirection.GRANT);

        // then
        assertScores(low, middle, 14, 18);
        assertScores(middle, high, 14, 18);
        assertThat(friendships.existsBetween(low, high)).isFalse();
        scores.applyFlagConclusion(low, List.of(middle, high), InteractionType.FLAG_PRESERVED, ScoreDirection.REVOKE);
        assertScores(low, middle, 0, 3);
        assertScores(middle, high, 0, 3);
    }

    @Test
    @DisplayName("같은 관계에 양쪽 상호작용을 동시에 적용해도 관심도와 친밀도가 일치한다")
    void applyInteraction_PreservesAllUpdates_ConcurrentDirections() throws Exception {
        // given
        create(low, high, 21.4, 30, OLD_INTERACTION, OLD_INTERACTION);
        CountDownLatch start = new CountDownLatch(1);

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> applyVisits(start, low, high));
            var second = workers.submit(() -> applyVisits(start, high, low));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }

        // then
        assertScores(low, high, 41.4, 50);
    }

    @Test
    @DisplayName("같은 행에서 양쪽 사용자의 설정 변경과 점수 쓰기가 겹쳐도 서로의 변경을 잃지 않는다")
    void updateFriendship_PreservesBothSidesAndScores_OnSameRow() throws Exception {
        // given
        create(low, high, 21.4, 30, OLD_INTERACTION, OLD_INTERACTION);
        CountDownLatch start = new CountDownLatch(1);

        // when
        try (var workers = Executors.newFixedThreadPool(3)) {
            var lowSettings = workers.submit(() -> {
                await(start);
                commands.updateSettings(low, high, new FriendshipUpdateCommand(low, high, "친구", true, null));
            });
            var highSettings = workers.submit(() -> {
                await(start);
                commands.updateSettings(high, low, new FriendshipUpdateCommand(high, low, null, null, false));
            });
            var score = workers.submit(() -> {
                await(start);
                scores.applyInteraction(high, low, InteractionType.VISIT);
            });
            start.countDown();
            for (var update : List.of(lowSettings, highSettings, score)) {
                update.get(15, TimeUnit.SECONDS);
            }
        }

        // then
        Friendship updated = assertScores(low, high, 21.4, 31);
        assertThat(updated.getFriendAlias(low)).isEqualTo("친구");
        assertThat(updated.isMuted(low)).isTrue();
        assertThat(updated.isRoutable(low)).isTrue();
        assertThat(updated.isMuted(high)).isFalse();
        assertThat(updated.isRoutable(high)).isFalse();
    }

    @Test
    @DisplayName("감쇠는 교류 시각이 기준 이하인 방향만 줄이고 기존 하한을 적용한다")
    void applyDecay_PreservesRecentAndBelowFloorScores() {
        // given
        create(low, middle, 1.1, 42, OLD_INTERACTION, RECENT_INTERACTION);
        create(middle, high, 0.5, 4, OLD_INTERACTION, OLD_INTERACTION);
        create(low, high, 1, 1, OLD_INTERACTION, OLD_INTERACTION);
        var transactions = new TransactionTemplate(transactionManager);

        // when
        transactions.executeWithoutResult(status -> friendships.applyDecay(0.5, 1, OLD_INTERACTION));

        // then
        var decayed = assertScores(low, middle, 1, 42);
        assertThat(decayed.getLower().getLastInteractedAt()).isEqualTo(OLD_INTERACTION);
        assertScores(middle, high, 0.5, 2);
        assertScores(low, high, 1, 1);
    }

    @Test
    @DisplayName("같은 입력에서 SQL 일괄 감쇠와 도메인 감쇠는 같은 관심도와 친밀도를 낸다")
    void applyDecay_MatchesDomainDecay_SameInput() {
        // given
        FriendshipDecayPolicy policy = new FriendshipDecayPolicy();
        double rate = policy.getDecayRate();
        double floor = policy.getMinThreshold();
        LocalDateTime decayTime = OLD_INTERACTION.plusDays(30);
        List<Friendship> inputs = List.of(
                Friendship.restore(CREATED_AT, recognition(low, 40, OLD_INTERACTION),
                        recognition(middle, 20, decayTime.plusSeconds(1))),
                Friendship.restore(CREATED_AT, recognition(middle, floor + 0.01, OLD_INTERACTION),
                        recognition(high, 0.5, OLD_INTERACTION)),
                Friendship.restore(CREATED_AT, recognition(low, floor, decayTime),
                        recognition(high, 63.2, decayTime)));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> inputs.forEach(friendships::create));
        List<Friendship> expected = List.of(
                Friendship.restore(CREATED_AT, recognition(low, 40, OLD_INTERACTION),
                        recognition(middle, 20, decayTime.plusSeconds(1))),
                Friendship.restore(CREATED_AT, recognition(middle, floor + 0.01, OLD_INTERACTION),
                        recognition(high, 0.5, OLD_INTERACTION)),
                Friendship.restore(CREATED_AT, recognition(low, floor, decayTime),
                        recognition(high, 63.2, decayTime)));
        expected.forEach(friendship -> friendship.decay(rate, floor, decayTime));
        var transactions = new TransactionTemplate(transactionManager);

        // when
        transactions.executeWithoutResult(status -> friendships.applyDecay(rate, floor, decayTime));

        // then
        for (Friendship domain : expected) {
            Long lowerId = domain.getLower().getOwnerId();
            Long higherId = domain.getHigher().getOwnerId();
            Friendship stored = friendships.findBetween(lowerId, higherId).orElseThrow();
            assertThat(stored.getMyInterestScore(lowerId))
                    .isCloseTo(domain.getMyInterestScore(lowerId), within(1e-9));
            assertThat(stored.getMyInterestScore(higherId))
                    .isCloseTo(domain.getMyInterestScore(higherId), within(1e-9));
            assertThat(intimacyColumn(lowerId, higherId)).isCloseTo(domain.getIntimacy(), within(1e-9));
        }
    }

    @Test
    @DisplayName("감쇠와 상호작용이 겹쳐도 새 교류 시각과 양쪽 점수가 보존된다")
    void processDecay_SerializesWithInteraction_OnSameRow() throws Exception {
        // given
        create(low, high, 21.4, 30, OLD_INTERACTION, OLD_INTERACTION);
        var transactions = new TransactionTemplate(transactionManager);
        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch decayStarted = new CountDownLatch(1);
        CountDownLatch finishInteraction = new CountDownLatch(1);

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var interaction = workers.submit(() -> transactions.executeWithoutResult(status -> {
                var locked = friendships.findBetweenForUpdate(low, high).stream().toList();
                rowLocked.countDown();
                await(finishInteraction);
                locked.getFirst().adjustInterestScore(high, 2);
                friendships.saveInterestScores(locked);
            }));
            try {
                await(rowLocked);
                var decay = workers.submit(() -> {
                    decayStarted.countDown();
                    scores.processDecay();
                });
                await(decayStarted);
                finishInteraction.countDown();
                interaction.get(10, TimeUnit.SECONDS);
                decay.get(10, TimeUnit.SECONDS);
            } finally {
                finishInteraction.countDown();
            }
        }

        // then
        assertScores(low, high, 21.4 * new FriendshipDecayPolicy().getDecayRate(), 32);
    }

    private void applyVisits(CountDownLatch start, Long actor, Long target) {
        await(start);
        for (int visit = 0; visit < 20; visit++) {
            scores.applyInteraction(actor, target, InteractionType.VISIT);
        }
    }

    private void create(Long first, Long second, double firstScore, double secondScore,
                        LocalDateTime firstTime, LocalDateTime secondTime) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendships.create(Friendship.restore(CREATED_AT,
                        recognition(first, firstScore, firstTime), recognition(second, secondScore, secondTime))));
    }

    private static FriendRecognition recognition(Long userId, double score, LocalDateTime lastInteraction) {
        return FriendRecognition.restore(userId, null, score, lastInteraction,
                false, true);
    }

    private Friendship assertScores(Long first, Long second, double firstScore, double secondScore) {
        Friendship friendship = friendships.findBetween(first, second).orElseThrow();
        assertThat(friendship.getMyInterestScore(first)).isCloseTo(firstScore, within(1e-9));
        assertThat(friendship.getMyInterestScore(second)).isCloseTo(secondScore, within(1e-9));
        assertThat(friendship.getIntimacy()).isCloseTo(intimacy(firstScore, secondScore), within(1e-9));
        assertThat(intimacyColumn(first, second)).isCloseTo(friendship.getIntimacy(), within(1e-9));
        return friendship;
    }

    private double intimacyColumn(Long first, Long second) {
        return jdbc.queryForObject("SELECT intimacy FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Double.class, Math.min(first, second), Math.max(first, second));
    }

    private static double intimacy(double firstScore, double secondScore) {
        return Math.sqrt(FriendRecognition.normalize(firstScore) * FriendRecognition.normalize(secondScore));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent score test did not reach its checkpoint");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
