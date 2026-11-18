package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.LabelJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.eventListener.LabelMembershipEventListener;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipConflictException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class, LabelPersistenceAdapter.class,
        LabelJdbcRepository.class, FriendshipCommandService.class,
        LabelCommandService.class, LabelNamer.class, LabelMemberRegistrar.class, LabelMembershipEventListener.class})
class FriendshipConcurrencyTest {

    private static final LocalDate FRIENDS_SINCE = LocalDate.of(2026, 1, 1);

    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private FriendshipCommandService friendshipCommandService;
    @Autowired private LabelCommandService labelCommandService;
    @Autowired private LabelJdbcRepository labelJdbcRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private FriendshipPersistenceAdapter friendshipPersistenceAdapter;

    private Long ownerId;
    private Long friendId;
    private String ownerLabelId;
    private String friendLabelId;

    @BeforeEach
    void setUp() {
        ownerId = userJpaRepository.save(User.createActive("owner@label-concurrency.test", "owner")).getId();
        friendId = userJpaRepository.save(User.createActive("friend@label-concurrency.test", "friend")).getId();
        ownerLabelId = labelCommandService.createLabel(ownerId, "내 라벨").getId();
        friendLabelId = labelCommandService.createLabel(friendId, "상대 라벨").getId();
    }

    @AfterEach
    void tearDown() {
        labelCommandService.deleteLabel(ownerId, ownerLabelId);
        labelCommandService.deleteLabel(friendId, friendLabelId);
        jdbcTemplate.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Math.min(ownerId, friendId), Math.max(ownerId, friendId));
        userJpaRepository.deleteAllById(List.of(ownerId, friendId));
    }

    @Test
    @DisplayName("owner의 추가가 먼저 공유 락을 잡으면 상대방의 친구 해제가 기다렸다가 새 멤버십도 지운다")
    void addMember_IsCleanedUp_WhenOwnerRegistersFirst() throws Exception {
        assertRegistrationLocksFirst(() -> labelCommandService.addMemberToLabel(ownerId, ownerLabelId, friendId));
    }

    @Test
    @DisplayName("owner의 교체가 먼저 공유 락을 잡으면 상대방의 친구 해제가 기다렸다가 새 멤버십도 지운다")
    void replaceMembers_IsCleanedUp_WhenOwnerRegistersFirst() throws Exception {
        assertRegistrationLocksFirst(() -> labelCommandService.replaceLabelMembers(ownerId, ownerLabelId, List.of(friendId)));
    }

    @Test
    @DisplayName("상대방이 먼저 친구를 끊으면 owner의 추가가 기다린 뒤 친구 관계 없음으로 실패한다")
    void addMember_Rejects_WhenFriendBreaksUpFirst() throws Exception {
        assertBreakupDeletesFirst(() -> labelCommandService.addMemberToLabel(ownerId, ownerLabelId, friendId),
                FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("상대방이 먼저 친구를 끊으면 owner의 교체가 기다린 뒤 비친구 등록으로 실패한다")
    void replaceMembers_Rejects_WhenFriendBreaksUpFirst() throws Exception {
        assertBreakupDeletesFirst(() -> labelCommandService.replaceLabelMembers(ownerId, ownerLabelId, List.of(friendId)),
                NonFriendLabelMemberException.class);
    }

    @Test
    @DisplayName("두 사용자가 서로를 각자의 라벨에 추가할 때 같은 친구 관계의 공유 락을 함께 잡는다")
    void addMember_SharesRelationshipLock_BothOwnersRegister() throws Exception {
        // given
        connect();
        var bothLocked = new CountDownLatch(2);
        var finishRegistrations = new CountDownLatch(1);
        doAnswer(call -> {
            Object candidates = call.callRealMethod();
            assertRepeatableRead();
            bothLocked.countDown();
            await(finishRegistrations);
            return candidates;
        }).when(friendshipPersistenceAdapter).findCandidatesWithSharedLock(anyLong(), anyCollection());

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var ownerRegistration = workers.submit(() -> labelCommandService.addMemberToLabel(ownerId, ownerLabelId, friendId));
            var friendRegistration = workers.submit(() -> labelCommandService.addMemberToLabel(friendId, friendLabelId, ownerId));
            try {
                await(bothLocked);
            } finally {
                finishRegistrations.countDown();
            }
            ownerRegistration.get(10, TimeUnit.SECONDS);
            friendRegistration.get(10, TimeUnit.SECONDS);
        }

        // then
        assertThat(labelJdbcRepository.existsMember(ownerLabelId, friendId)).isTrue();
        assertThat(labelJdbcRepository.existsMember(friendLabelId, ownerId)).isTrue();
    }

    @Test
    @DisplayName("설정을 바꾸려고 읽은 관계가 저장 전에 끊기면 설정 저장이 0건이라 충돌로 끝난다")
    void updateFriendship_ConflictsOnZeroRowUpdate_DeletedAfterLoad() {
        // given
        connect();
        doAnswer(call -> {
            Object loaded = call.callRealMethod();
            jdbcTemplate.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                    Math.min(ownerId, friendId), Math.max(ownerId, friendId));
            return loaded;
        }).when(friendshipPersistenceAdapter).findBetween(ownerId, friendId);

        // when & then
        assertThatThrownBy(() -> friendshipCommandService.updateSettings(ownerId, friendId,
                new FriendshipUpdateCommand(ownerId, friendId, "친구", null, null)))
                .isInstanceOf(FriendshipConflictException.class);
    }

    private void assertRegistrationLocksFirst(Runnable registration) throws Exception {
        connect();
        var registrationLocked = new CountDownLatch(1);
        var finishRegistration = new CountDownLatch(1);
        var breakupEntered = new CountDownLatch(1);
        doAnswer(call -> {
            Object candidates = call.callRealMethod();
            assertRepeatableRead();
            registrationLocked.countDown();
            await(finishRegistration);
            return candidates;
        }).when(friendshipPersistenceAdapter).findCandidatesWithSharedLock(ownerId, List.of(friendId));
        doAnswer(call -> {
            assertRepeatableRead();
            breakupEntered.countDown();
            return call.callRealMethod();
        }).when(friendshipPersistenceAdapter).deleteBetween(friendId, ownerId);

        try (var workers = Executors.newFixedThreadPool(2)) {
            var addition = workers.submit(registration);
            try {
                await(registrationLocked);
                var breakup = workers.submit(() -> friendshipCommandService.endFriendship(friendId, ownerId));
                await(breakupEntered);
                assertWaiting(breakup);
                finishRegistration.countDown();
                addition.get(10, TimeUnit.SECONDS);
                breakup.get(10, TimeUnit.SECONDS);
            } finally {
                finishRegistration.countDown();
            }
        }

        assertEnded();
    }

    private void assertBreakupDeletesFirst(Runnable registration, Class<? extends RuntimeException> expected) throws Exception {
        connectWithBothMemberships();
        var deleted = new CountDownLatch(1);
        var finishBreakup = new CountDownLatch(1);
        var registrationEntered = new CountDownLatch(1);
        doAnswer(call -> {
            Object result = call.callRealMethod();
            assertRepeatableRead();
            deleted.countDown();
            await(finishBreakup);
            return result;
        }).when(friendshipPersistenceAdapter).deleteBetween(friendId, ownerId);
        doAnswer(call -> {
            assertRepeatableRead();
            registrationEntered.countDown();
            return call.callRealMethod();
        }).when(friendshipPersistenceAdapter).findCandidatesWithSharedLock(ownerId, List.of(friendId));

        try (var workers = Executors.newFixedThreadPool(2)) {
            var breakup = workers.submit(() -> friendshipCommandService.endFriendship(friendId, ownerId));
            try {
                await(deleted);
                var addition = workers.submit(() -> assertThatThrownBy(registration::run).isInstanceOf(expected));
                await(registrationEntered);
                assertWaiting(addition);
                finishBreakup.countDown();
                breakup.get(10, TimeUnit.SECONDS);
                addition.get(10, TimeUnit.SECONDS);
            } finally {
                finishBreakup.countDown();
            }
        }

        assertEnded();
    }

    private void connect() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendshipPersistenceAdapter.create(Friendship.restore(FRIENDS_SINCE,
                        FriendRecognition.restore(ownerId, null, FriendRecognition.INITIAL_RAW_SCORE,
                                LocalDateTime.now(), false, true),
                        FriendRecognition.restore(friendId, null, FriendRecognition.INITIAL_RAW_SCORE,
                                LocalDateTime.now(), false, true))));
    }

    private void connectWithBothMemberships() {
        connect();
        labelCommandService.addMemberToLabel(ownerId, ownerLabelId, friendId);
        labelCommandService.addMemberToLabel(friendId, friendLabelId, ownerId);
    }

    private void assertEnded() {
        assertThat(friendshipPersistenceAdapter.existsBetween(ownerId, friendId)).isFalse();
        assertThat(labelJdbcRepository.existsMember(ownerLabelId, friendId)).isFalse();
        assertThat(labelJdbcRepository.existsMember(friendLabelId, ownerId)).isFalse();
    }

    private void assertRepeatableRead() {
        assertThat(jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class))
                .isEqualTo("REPEATABLE-READ");
    }

    private static void assertWaiting(Future<?> request) {
        assertThatThrownBy(() -> request.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent request did not reach the expected database operation");
        }
    }
}
