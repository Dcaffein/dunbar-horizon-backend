package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.UserProfileJdbcRepository;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.MySqlLabelService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class, LabelMysqlRepository.class, UserProfileJdbcRepository.class,
        FriendshipCommandService.class, MySqlLabelService.class, LabelFactory.class, LabelNamePolicy.class})
class FriendshipLifecycleMysqlTest {

    private static final LocalDate FRIENDS_SINCE = LocalDate.of(2026, 1, 1);

    @Autowired private UserJpaRepository users;
    @Autowired private FriendshipCommandService lifecycle;
    @Autowired private MySqlLabelService labelService;
    @MockitoSpyBean private LabelMysqlRepository labels;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private FriendshipPersistenceAdapter friendships;

    private Long first;
    private Long second;
    private String firstLabel;
    private String secondLabel;

    @BeforeEach
    void setUp() {
        first = users.save(User.createActive("first@mysql-friendship-lifecycle.test", "first")).getId();
        second = users.save(User.createActive("second@mysql-friendship-lifecycle.test", "second")).getId();
        firstLabel = labelService.createLabel(first, "내 라벨").getId();
        secondLabel = labelService.createLabel(second, "상대 라벨").getId();
    }

    @AfterEach
    void tearDown() {
        labels.delete(firstLabel);
        labels.delete(secondLabel);
        jdbc.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Math.min(first, second), Math.max(first, second));
        users.deleteAllById(List.of(first, second));
    }

    @Test
    @DisplayName("친구 관계를 끊으면 양쪽 라벨에서 서로 제거된다")
    void brokeUpWith_RemovesBothMemberships_WithFriendship() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        lifecycle.updateFriendship(second, first, new FriendshipUpdateCommand(second, first, "친구", null, null));
        assertThat(friendships.findBetween(first, second).orElseThrow().getFriendAlias(second)).isEqualTo("친구");

        // when
        lifecycle.brokeUpWith(second, first);

        // then
        assertEnded();
        assertThatThrownBy(() -> lifecycle.brokeUpWith(first, second)).isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("친구 끊기에서 쓰기 중 실패하면 관계와 양쪽 라벨 멤버십이 복원된다")
    void brokeUpWith_RollsBackBothMemberships_OnFailure() {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        failAfterLabelCleanup();

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
    @DisplayName("친구 끊기가 먼저 관계를 삭제하면 뒤따르는 멤버 교체는 관계가 없음을 확인한다")
    void replaceMembers_RejectsEndedFriend_WhenBreakupDeletesFirst() throws Exception {
        // given
        connectWithBothMemberships(FriendRecognition.INITIAL_RAW_SCORE);
        var deleted = new CountDownLatch(1);
        var finishBreakup = new CountDownLatch(1);
        var replacementStarted = new CountDownLatch(1);
        doAnswer(call -> {
            Object result = call.callRealMethod();
            deleted.countDown();
            await(finishBreakup);
            return result;
        }).when(friendships).deleteBetween(second, first);

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var breakup = workers.submit(() -> lifecycle.brokeUpWith(second, first));
            try {
                await(deleted);
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

    private void failAfterLabelCleanup() {
        doAnswer(call -> {
            call.callRealMethod();
            throw new IllegalStateException("interrupted after label cleanup");
        }).when(labels).removeMembersBetween(first, second);
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

    @Test
    @DisplayName("설정을 바꾸려고 읽은 관계가 커밋 전에 끊기면 설정 변경은 동시 수정 실패로 끝난다")
    void updateFriendship_FailsConcurrentModification_DeletedAfterLoad() {
        // given
        connect(FriendRecognition.INITIAL_RAW_SCORE);
        doAnswer(call -> {
            Object loaded = call.callRealMethod();
            jdbc.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                    Math.min(first, second), Math.max(first, second));
            return loaded;
        }).when(friendships).findBetween(first, second);

        // when & then
        assertThatThrownBy(() -> lifecycle.updateFriendship(first, second,
                new FriendshipUpdateCommand(first, second, "친구", null, null)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    private void connect(double interestScore) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendships.create(Friendship.restore(FRIENDS_SINCE,
                        FriendRecognition.restore(first, null, interestScore, LocalDateTime.now(), false, true),
                        FriendRecognition.restore(second, null, interestScore, LocalDateTime.now(), false, true))));
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
