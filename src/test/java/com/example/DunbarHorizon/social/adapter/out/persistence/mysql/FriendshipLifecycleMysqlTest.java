package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendshipJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.MySqlLabelService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchivePolicy;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.exception.InvalidFriendAliasException;
import com.example.DunbarHorizon.social.domain.label.LabelFactory;
import com.example.DunbarHorizon.social.domain.label.LabelNamePolicy;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipMysqlRepository.class, LabelMysqlRepository.class, SocialUserMysqlRepository.class,
        FriendshipCommandService.class, MySqlLabelService.class, LabelFactory.class, LabelNamePolicy.class,
        ArchivedFriendshipRepositoryAdapter.class, FriendshipArchivePolicy.class, FriendshipDecayPolicy.class})
class FriendshipLifecycleMysqlTest {

    private static final LocalDate FRIENDS_SINCE = LocalDate.of(2026, 1, 1);

    @Autowired private UserJpaRepository users;
    @Autowired private ArchivedFriendshipJpaRepository archives;
    @Autowired private FriendshipCommandService lifecycle;
    @Autowired private MySqlLabelService labelService;
    @Autowired private LabelMysqlRepository labels;
    @Autowired private FriendshipDecayPolicy decayPolicy;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private FriendshipMysqlRepository friendships;
    @MockitoSpyBean private NamedParameterJdbcTemplate namedJdbc;

    private Long first;
    private Long second;
    private String pairId;
    private String firstLabel;
    private String secondLabel;

    @BeforeEach
    void setUp() {
        first = users.save(User.createActive("first@mysql-friendship-lifecycle.test", "first")).getId();
        second = users.save(User.createActive("second@mysql-friendship-lifecycle.test", "second")).getId();
        pairId = Math.min(first, second) + "_" + Math.max(first, second);
        firstLabel = labelService.createLabel(first, "내 라벨").getId();
        secondLabel = labelService.createLabel(second, "상대 라벨").getId();
    }

    @AfterEach
    void tearDown() {
        labels.delete(firstLabel);
        labels.delete(secondLabel);
        jdbc.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Math.min(first, second), Math.max(first, second));
        archives.deleteById(pairId);
        users.deleteAllById(List.of(first, second));
    }

    @Test
    @DisplayName("친구 관계를 끊으면 양쪽 라벨에서 서로 제거되고 별도 아카이브는 남지 않는다")
    void brokeUpWith_RemovesBothMemberships_WithFriendship() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        lifecycle.updateFriendship(second, first, new FriendshipUpdateCommand(second, first, "친구", null, null));
        assertThat(friendships.findBetween(first, second).orElseThrow().getFriendAlias(second)).isEqualTo("친구");

        // when
        lifecycle.brokeUpWith(second, first);

        // then
        assertEnded();
        assertThat(archives.findById(pairId)).isEmpty();
        assertThatThrownBy(() -> lifecycle.brokeUpWith(first, second)).isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("친구 끊기에서 쓰기 중 실패하면 관계와 양쪽 라벨 멤버십이 복원된다")
    void brokeUpWith_RollsBackBothMemberships_OnFailure() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        failAfterDeletion();

        // when & then
        assertThatThrownBy(() -> lifecycle.brokeUpWith(first, second)).isInstanceOf(IllegalStateException.class);
        assertRelationshipAndMembershipsRemain();
    }

    @Test
    @DisplayName("멤버 추가가 먼저 관계를 잠그면 뒤따르는 친구 끊기가 새 멤버십도 삭제한다")
    void brokeUpWith_RemovesJustAddedMembership_WhenAdditionLocksFirst() throws Exception {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);
        var additionLocked = new CountDownLatch(1);
        var resumeAddition = new CountDownLatch(1);
        var breakupStarted = new CountDownLatch(1);
        doAnswer(call -> {
            boolean exists = (boolean) call.callRealMethod();
            additionLocked.countDown();
            await(resumeAddition);
            return exists;
        }).when(friendships).existsFriendshipForUpdate(first, second);

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var addition = workers.submit(() -> labelService.addMemberToLabel(first, firstLabel, second));
            try {
                await(additionLocked);
                var breakup = workers.submit(() -> {
                    breakupStarted.countDown();
                    lifecycle.brokeUpWith(second, first);
                });
                await(breakupStarted);
                resumeAddition.countDown();
                addition.get(10, TimeUnit.SECONDS);
                breakup.get(10, TimeUnit.SECONDS);
            } finally {
                resumeAddition.countDown();
            }
        }

        // then
        assertEnded();
    }

    @Test
    @DisplayName("친구 끊기가 먼저 관계를 잠그면 뒤따르는 멤버 교체는 관계가 없음을 확인한다")
    void replaceMembers_RejectsEndedFriend_WhenBreakupLocksFirst() throws Exception {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        var beforeDelete = new CountDownLatch(1);
        var finishBreakup = new CountDownLatch(1);
        var replacementStarted = new CountDownLatch(1);
        doAnswer(call -> {
            beforeDelete.countDown();
            await(finishBreakup);
            return call.callRealMethod();
        }).when(friendships).deleteAll(anyCollection());

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var breakup = workers.submit(() -> lifecycle.brokeUpWith(second, first));
            try {
                await(beforeDelete);
                var replacement = workers.submit(() -> {
                    replacementStarted.countDown();
                    assertThatThrownBy(() -> labelService.replaceLabelMembers(first, firstLabel, List.of(second)))
                            .isInstanceOf(NonFriendLabelMemberException.class);
                });
                await(replacementStarted);
                finishBreakup.countDown();
                breakup.get(10, TimeUnit.SECONDS);
                replacement.get(10, TimeUnit.SECONDS);
            } finally {
                finishBreakup.countDown();
            }
        }

        // then
        assertEnded();
    }

    @Test
    @DisplayName("하한에 도달한 관계의 아카이브 기록과 양쪽 라벨 정리를 함께 커밋한다")
    void archiveFriendships_CommitsRecordAndCleanup_AtThreshold() {
        // given
        connectWithBothMemberships(decayPolicy.getMinThreshold());

        // when
        lifecycle.archiveFriendships();

        // then
        assertEnded();
        assertThat(archives.findById(pairId)).hasValueSatisfying(record -> {
            assertThat(record.getUserAId()).isEqualTo(first);
            assertThat(record.getUserBId()).isEqualTo(second);
            assertThat(record.getFriendedAt()).isEqualTo(FRIENDS_SINCE);
        });
    }

    @Test
    @DisplayName("아카이브 중 쓰기가 실패하면 신규 기록과 관계·멤버십 정리도 함께 롤백된다")
    void archiveFriendships_RollsBackRecordAndCleanup_OnFailure() {
        // given
        connectWithBothMemberships(decayPolicy.getMinThreshold());
        failAfterDeletion();

        // when & then
        assertThatThrownBy(lifecycle::archiveFriendships).isInstanceOf(IllegalStateException.class);
        assertThat(archives.findById(pairId)).isEmpty();
        assertRelationshipAndMembershipsRemain();
    }

    @Test
    @DisplayName("후보 조회 뒤 친밀도가 회복하면 잠금 조회에서 확인하고 아카이브하지 않는다")
    void archiveFriendships_PreservesRelationship_WhenIntimacyRecovered() {
        // given
        connectWithBothMemberships(decayPolicy.getMinThreshold());
        doAnswer(call -> {
            Object candidates = call.callRealMethod();
            try (var worker = Executors.newSingleThreadExecutor()) {
                worker.submit(() -> jdbc.update("""
                        UPDATE friendships SET low_interest_score = 30, high_interest_score = 30, intimacy = 0.375
                        WHERE low_user_id = ? AND high_user_id = ?
                        """, Math.min(first, second), Math.max(first, second))).get(10, TimeUnit.SECONDS);
            }
            return candidates;
        }).when(namedJdbc).query(argThat((String sql) -> sql != null && sql.contains("intimacy <= :threshold")),
                anyMap(), any(RowMapper.class));

        // when
        lifecycle.archiveFriendships();

        // then
        assertThat(archives.findById(pairId)).isEmpty();
        assertRelationshipAndMembershipsRemain();
    }

    private void failAfterDeletion() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("interrupted after relationship deletion");
        }).when(friendships).deleteAll(anyCollection());
    }

    @Test
    @DisplayName("관계가 없거나 같은 사용자를 지정하면 설정 변경은 관계 없음 예외로 끝난다")
    void updateFriendship_ThrowsNotFound_MissingRelationship() {
        assertThatThrownBy(() -> lifecycle.updateFriendship(first, second,
                new FriendshipUpdateCommand(first, second, null, true, null)))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThatThrownBy(() -> lifecycle.updateFriendship(first, first,
                new FriendshipUpdateCommand(first, first, null, null, null)))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("별명 검증에 실패하면 함께 요청한 다른 설정도 바뀌지 않는다")
    void updateFriendship_RejectsEntireUpdate_InvalidAlias() {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);

        // when & then
        assertThatThrownBy(() -> lifecycle.updateFriendship(second, first, new FriendshipUpdateCommand(second, first,
                "a".repeat(Friendship.ALIAS_MAX_LENGTH + 1), true, null)))
                .isInstanceOf(InvalidFriendAliasException.class);
        assertThat(friendships.findBetween(first, second).orElseThrow().isMuted(second)).isFalse();
    }

    @Test
    @DisplayName("설정 변경은 요청한 필드만 바꾸고 빈 별명은 별명을 지운다")
    void updateFriendship_ChangesRequestedFields_ClearsEmptyAlias() {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);
        lifecycle.updateFriendship(first, second, new FriendshipUpdateCommand(first, second, "친구", true, null));

        // when
        lifecycle.updateFriendship(first, second, new FriendshipUpdateCommand(first, second, "", null, false));

        // then
        Friendship updated = friendships.findBetween(first, second).orElseThrow();
        assertThat(updated.getFriendAlias(first)).isEmpty();
        assertThat(updated.isMuted(first)).isTrue();
        assertThat(updated.isRoutable(first)).isFalse();
        assertThat(updated.isMuted(second)).isFalse();
        assertThat(updated.isRoutable(second)).isTrue();
    }

    private void connect(double interestScore) {
        friendships.create(Friendship.restore(FRIENDS_SINCE,
                FriendRecognition.restore(first, null, interestScore, LocalDateTime.now(), false, true),
                FriendRecognition.restore(second, null, interestScore, LocalDateTime.now(), false, true)));
    }

    private void connectWithBothMemberships(double interestScore) {
        connect(interestScore);
        labelService.addMemberToLabel(first, firstLabel, second);
        labelService.addMemberToLabel(second, secondLabel, first);
    }

    private void assertEnded() {
        assertThat(friendships.existsFriendshipBetween(first, second)).isFalse();
        assertThat(labels.existsMember(firstLabel, second)).isFalse();
        assertThat(labels.existsMember(secondLabel, first)).isFalse();
    }

    private void assertRelationshipAndMembershipsRemain() {
        assertThat(friendships.existsFriendshipBetween(first, second)).isTrue();
        assertThat(labels.existsMember(firstLabel, second)).isTrue();
        assertThat(labels.existsMember(secondLabel, first)).isTrue();
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent lifecycle test did not reach its checkpoint");
        }
    }
}
