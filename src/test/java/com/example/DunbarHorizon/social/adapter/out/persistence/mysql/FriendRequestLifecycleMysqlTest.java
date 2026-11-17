package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.FriendRequestPersistenceAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.UserProfileJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.service.FriendRequestService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
@ImportAutoConfiguration(PersistenceExceptionTranslationAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendRequestPersistenceAdapter.class, FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class,
        FriendRequestService.class,
        UserProfileJdbcRepository.class,
        FriendRequestLifecycleMysqlTest.AcceptedEvents.class})
class FriendRequestLifecycleMysqlTest {

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
    @DisplayName("높은 ID에서 낮은 ID로 보낸 요청의 방향, 상태, 조회와 취소를 유지한다")
    void changeRequest_PreservesDirectionAndState_HighRequester() {
        // given
        friendRequestService.sendRequest(high, low);

        // when
        friendRequestService.updateStatus(low, high, FriendRequestStatus.HIDDEN);

        // then
        assertThat(friendRequestPersistenceAdapter.findAllByRequesterId(high)).singleElement()
                .extracting(FriendRequest::getStatus).isEqualTo(FriendRequestStatus.HIDDEN);
        assertThat(friendRequestPersistenceAdapter.findAllByReceiverIdAndStatus(low, FriendRequestStatus.HIDDEN)).singleElement()
                .satisfies(request -> {
                    assertThat(request.getRequesterId()).isEqualTo(high);
                    assertThat(request.getReceiverId()).isEqualTo(low);
                });
        friendRequestService.updateStatus(low, high, FriendRequestStatus.PENDING);
        assertThat(friendRequestPersistenceAdapter.findAllByRequesterId(high)).singleElement()
                .extracting(FriendRequest::getStatus).isEqualTo(FriendRequestStatus.PENDING);
        friendRequestService.cancelRequest(high, low);
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
    }

    @Test
    @DisplayName("수락이 커밋되면 요청이 사라지고 양방향 친구 관계와 수락 이벤트가 생긴다")
    void accept_CommitsFriendshipAndRemoval_PendingRequest() {
        // given
        friendRequestService.sendRequest(high, low);

        // when
        friendRequestService.updateStatus(low, high, FriendRequestStatus.ACCEPTED);

        // then
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(friendshipPersistenceAdapter.findByUserId(low)).hasSize(1);
        assertThat(friendshipPersistenceAdapter.findByUserId(high)).hasSize(1);
        assertThat(acceptedEvents.events).containsExactly(new FriendRequestAcceptedEvent(high, low, "low"));
    }

    @Test
    @DisplayName("관계 생성과 요청 삭제 뒤 실패하면 두 쓰기가 함께 롤백된다")
    void accept_RollsBackBothWrites_ErrorAfterFriendshipCreated() {
        // given
        friendRequestService.sendRequest(low, high);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("acceptance interrupted after both writes");
        }).when(friendshipPersistenceAdapter).create(any(Friendship.class));

        // when & then
        assertThatThrownBy(() -> friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
    }

    @Test
    @DisplayName("관계 확인을 지나쳐도 중복 관계로 수락이 실패하면 요청이 남고 수락 이벤트가 없다")
    void accept_KeepsRequest_DuplicateFriendshipKey() {
        // given
        friendRequestService.sendRequest(low, high);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                friendshipPersistenceAdapter.create(Friendship.restore(LocalDate.of(2026, 1, 1),
                        FriendRecognition.restore(low, "기존", 21.4, null, false, true),
                        FriendRecognition.restore(high, null, 21.4, null, false, true))));
        doReturn(false).when(friendshipPersistenceAdapter).existsFriendshipBetween(low, high);

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
    @DisplayName("두 방향이 사전 검사를 함께 통과해도 한 요청만 생성된다")
    void sendRequest_AllowsOnlyOne_OppositeDirectionsRace() throws Exception {
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
    @DisplayName("취소가 먼저 커밋되면 뒤따른 수락은 동시 수정으로 실패하고 관계도 남지 않는다")
    void accept_FailsAndRollsBack_CancelCommittedFirst() throws Exception {
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
            assertThat(accept.get(10, TimeUnit.SECONDS)).isEqualTo(ObjectOptimisticLockingFailureException.class);
        }
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(acceptedEvents.events).isEmpty();
    }

    @Test
    @DisplayName("수락이 먼저 커밋되면 뒤따른 취소는 동시 수정으로 실패하고 관계는 남는다")
    void cancel_FailsConcurrentModification_AcceptanceCommittedFirst() throws Exception {
        // given
        friendRequestService.sendRequest(low, high);
        CountDownLatch cancelLoaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicBoolean firstDelete = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (firstDelete.getAndSet(false)) {
                cancelLoaded.countDown();
                await(resume);
            }
            return result;
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
            assertThat(cancel.get(10, TimeUnit.SECONDS)).isEqualTo(ObjectOptimisticLockingFailureException.class);
        }
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isTrue();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
    }

    @Test
    @DisplayName("같은 요청을 동시에 두 번 수락하면 한 번만 성공한다")
    void accept_SucceedsOnce_ConcurrentAcceptances() throws Exception {
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
                .isIn(AlreadyFriendsException.class, ObjectOptimisticLockingFailureException.class);
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isTrue();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(acceptedEvents.events).hasSize(1);
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
