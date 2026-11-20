package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestResult;
import com.example.DunbarHorizon.social.application.friendrequest.FriendRequestService;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequest;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friendship.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friendship.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friendship.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendRequestConflictException;
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
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendRequestPersistenceAdapter.class, FriendshipPersistenceAdapter.class,
        FriendRequestService.class, FriendshipBroker.class, FriendRequestQueryJdbcAdapter.class,
        FriendRequestConcurrencyTest.AcceptedEvents.class})
class FriendRequestConcurrencyTest {

    @Autowired private FriendRequestService friendRequestService;
    @MockitoSpyBean private FriendshipPersistenceAdapter friendshipPersistenceAdapter;
    @MockitoSpyBean private FriendRequestPersistenceAdapter friendRequestPersistenceAdapter;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AcceptedEvents acceptedEvents;

    private Long low;
    private Long high;

    @BeforeEach
    void setUp() {
        low = userJpaRepository.save(User.createActive("low@mysql-requests.test", "low")).getId();
        high = userJpaRepository.save(User.createActive("high@mysql-requests.test", "high")).getId();
        acceptedEvents.events.clear();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM friend_requests WHERE requester_id IN (?, ?)", low, high);
        jdbcTemplate.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?", low, high);
        userJpaRepository.deleteAllById(List.of(low, high));
    }

    @Test
    @DisplayName("관계 확인을 지나친 수락은 친구 관계 PK에 막혀 중복 친구로 끝나고, 요청은 남고 수락 이벤트는 없다")
    void accept_BlockedByFriendshipPrimaryKey_SkippedFriendshipCheck() {
        // given
        friendRequestService.sendRequest(low, high);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendshipPersistenceAdapter.create(Friendship.restore(LocalDate.of(2026, 1, 1),
                        FriendRecognition.restore(low, "기존", 21.4, null, false, true),
                        FriendRecognition.restore(high, null, 21.4, null, false, true))));
        doReturn(false).when(friendshipPersistenceAdapter).existsBetween(low, high);

        // when & then
        assertThatThrownBy(() -> friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED))
                .isInstanceOf(AlreadyFriendsException.class);
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
        assertThat(friendshipPersistenceAdapter.findBetween(low, high)).get()
                .extracting(friendship -> friendship.getFriendAlias(low)).isEqualTo("기존");
    }

    @Test
    @DisplayName("반대 방향 요청이 함께 사전 검사를 통과해도 쌍 유니크 제약이 하나만 남긴다")
    void sendRequest_PairUniqueKeyKeepsOne_OppositeDirectionsRace() throws Exception {
        // given
        CountDownLatch beforeInsert = new CountDownLatch(2);
        doAnswer(invocation -> {
            beforeInsert.countDown();
            await(beforeInsert);
            return invocation.callRealMethod();
        }).when(friendRequestPersistenceAdapter).create(any(FriendRequest.class));

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(outcome(() -> friendRequestService.sendRequest(low, high)));
            var second = workers.submit(outcome(() -> friendRequestService.sendRequest(high, low)));

            // then
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(FriendRequestResult.class, DuplicateFriendRequestException.class);
        }
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isPresent();
    }

    @Test
    @DisplayName("취소가 먼저 커밋되면 뒤따른 수락은 요청 삭제가 0건이라 충돌로 끝나고 관계도 남지 않는다")
    void accept_ConflictsOnZeroRowDelete_CancelCommittedFirst() throws Exception {
        // given
        friendRequestService.sendRequest(low, high);
        CountDownLatch beforeDelete = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean firstDelete = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (firstDelete.getAndSet(false)) {
                beforeDelete.countDown();
                await(resume);
            }
            return invocation.callRealMethod();
        }).when(friendRequestPersistenceAdapter).delete(any(FriendRequest.class));

        // when
        try (var workers = Executors.newSingleThreadExecutor()) {
            var accept = workers.submit(outcome(() -> {
                friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED);
                return null;
            }));
            try {
                await(beforeDelete);
                friendRequestService.cancelRequest(low, high);
            } finally {
                resume.countDown();
            }

            // then
            assertThat(accept.get(10, TimeUnit.SECONDS)).isEqualTo(FriendRequestConflictException.class);
        }
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(acceptedEvents.events).isEmpty();
    }

    @Test
    @DisplayName("수락이 먼저 커밋되면 뒤따른 취소는 요청 삭제가 0건이라 충돌로 끝나고 관계는 남는다")
    void cancel_ConflictsOnZeroRowDelete_AcceptanceCommittedFirst() throws Exception {
        // given
        friendRequestService.sendRequest(low, high);
        CountDownLatch cancelLoaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean firstDelete = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (firstDelete.getAndSet(false)) {
                cancelLoaded.countDown();
                await(resume);
            }
            return invocation.callRealMethod();
        }).when(friendRequestPersistenceAdapter).delete(any(FriendRequest.class));

        // when
        try (var workers = Executors.newSingleThreadExecutor()) {
            var cancel = workers.submit(outcome(() -> {
                friendRequestService.cancelRequest(low, high);
                return null;
            }));
            try {
                await(cancelLoaded);
                friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED);
            } finally {
                resume.countDown();
            }

            // then
            assertThat(cancel.get(10, TimeUnit.SECONDS)).isEqualTo(FriendRequestConflictException.class);
        }
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isTrue();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
    }

    @Test
    @DisplayName("같은 요청을 동시에 두 번 수락하면 뒤쪽은 요청 행 잠금을 기다린 뒤 삭제가 0건이라 충돌로 끝난다")
    void accept_SecondConflictsOnZeroRowDelete_ConcurrentAcceptances() throws Exception {
        // given
        friendRequestService.sendRequest(low, high);
        CountDownLatch beforeDelete = new CountDownLatch(2);
        doAnswer(invocation -> {
            beforeDelete.countDown();
            await(beforeDelete);
            return invocation.callRealMethod();
        }).when(friendRequestPersistenceAdapter).delete(any(FriendRequest.class));

        // when
        List<Class<?>> outcomes;
        try (var workers = Executors.newFixedThreadPool(2)) {
            Callable<Class<?>> accept = outcome(() -> {
                friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED);
                return null;
            });
            var first = workers.submit(accept);
            var second = workers.submit(accept);
            outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }

        // then
        assertThat(outcomes).filteredOn(Void.class::equals).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !Void.class.equals(outcome)).singleElement()
                .isEqualTo(FriendRequestConflictException.class);
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isTrue();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(acceptedEvents.events).hasSize(1);
    }

    @Test
    @DisplayName("취소가 먼저 커밋되면 뒤따른 숨김은 상태 저장이 0건이라 충돌로 끝난다")
    void hide_ConflictsOnZeroRowUpdate_CancelCommittedFirst() throws Exception {
        // given
        friendRequestService.sendRequest(low, high);
        CountDownLatch beforeSave = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        doAnswer(invocation -> {
            beforeSave.countDown();
            await(resume);
            return invocation.callRealMethod();
        }).when(friendRequestPersistenceAdapter).saveStatus(any(FriendRequest.class));

        // when
        try (var workers = Executors.newSingleThreadExecutor()) {
            var hide = workers.submit(outcome(() -> {
                friendRequestService.updateStatus(high, low, FriendRequestStatus.HIDDEN);
                return null;
            }));
            try {
                await(beforeSave);
                friendRequestService.cancelRequest(low, high);
            } finally {
                resume.countDown();
            }

            // then
            assertThat(hide.get(10, TimeUnit.SECONDS)).isEqualTo(FriendRequestConflictException.class);
        }
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
    }

    private static Callable<Class<?>> outcome(Callable<?> operation) {
        return () -> {
            try {
                Object result = operation.call();
                return result == null ? Void.class : result.getClass();
            } catch (RuntimeException failure) {
                return failure.getClass();
            }
        };
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent request test did not reach its checkpoint");
        }
    }

    static class AcceptedEvents {
        private final ConcurrentLinkedQueue<FriendRequestAcceptedEvent> events = new ConcurrentLinkedQueue<>();

        @TransactionalEventListener
        void record(FriendRequestAcceptedEvent event) {
            events.add(event);
        }
    }
}
