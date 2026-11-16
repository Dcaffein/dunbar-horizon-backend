package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.application.service.MySqlFriendRequestActionService;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendRequestMysqlRepository.class, FriendshipMysqlRepository.class, FriendshipRepositoryAdapter.class,
        MySqlFriendRequestActionService.class,
        SocialUserMysqlRepository.class,
        FriendRequestMysqlRepositoryTest.AcceptedEvents.class})
class FriendRequestMysqlRepositoryTest {

    @Autowired private MySqlFriendRequestActionService actions;
    @MockitoSpyBean private FriendshipMysqlRepository friendshipLocks;
    @Autowired private FriendshipRepositoryAdapter friendships;
    @Autowired private UserJpaRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AcceptedEvents acceptedEvents;
    @MockitoSpyBean private FriendRequestMysqlRepository requests;

    private Long low;
    private Long high;
    private String requestId;

    @BeforeEach
    void setUp() {
        low = users.save(User.createActive("low@mysql-requests.test", "low")).getId();
        high = users.save(User.createActive("high@mysql-requests.test", "high")).getId();
        requestId = FriendRequest.generateId(low, high);
        acceptedEvents.events.clear();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM friend_requests WHERE low_user_id = ? AND high_user_id = ?", low, high);
        jdbc.update("DELETE FROM friendships WHERE low_user_id = ? AND high_user_id = ?", low, high);
        users.deleteAllById(List.of(low, high));
    }

    @Test
    @DisplayName("높은 ID에서 낮은 ID로 보낸 요청의 방향, 상태, 조회와 취소를 유지한다")
    void changeRequest_PreservesDirectionAndState_HighRequester() {
        // given
        actions.sendRequest(high, low);

        // when
        actions.updateStatus(low, high, FriendRequestStatus.HIDDEN);

        // then
        assertThat(requests.findAllByRequester_IdAndStatus(high, FriendRequestStatus.PENDING)).isEmpty();
        assertThat(requests.findAllByReceiver_IdAndStatus(low, FriendRequestStatus.HIDDEN)).singleElement()
                .satisfies(request -> {
                    assertThat(request.getId()).isEqualTo(requestId);
                    assertThat(request.getRequesterId()).isEqualTo(high);
                    assertThat(request.getReceiverId()).isEqualTo(low);
                });
        assertThat(requests.existsRequestBetween(high, low)).isTrue();
        assertThat(requests.existsRequestBetween(low, high)).isFalse();
        actions.updateStatus(low, high, FriendRequestStatus.PENDING);
        assertThat(requests.findAllByRequester_IdAndStatus(high, FriendRequestStatus.PENDING)).hasSize(1);
        actions.cancelRequest(low, high);
        assertThat(requests.findById(requestId)).isEmpty();
    }

    @Test
    @DisplayName("오래된 대기 요청은 유지하고 만료 시각에 도달한 숨김 요청은 제거한다")
    void deleteOldHiddenRequests_DeletesAtThreshold_HiddenOnly() {
        // given
        actions.sendRequest(low, high);
        LocalDateTime threshold = requests.findById(requestId).orElseThrow().getCreatedAt();

        // when & then
        requests.deleteOldHiddenRequests(threshold);
        assertThat(requests.findById(requestId)).isPresent();
        actions.updateStatus(high, low, FriendRequestStatus.HIDDEN);
        requests.deleteOldHiddenRequests(threshold.minusSeconds(1));
        assertThat(requests.findById(requestId)).isPresent();
        requests.deleteOldHiddenRequests(threshold);
        assertThat(requests.findById(requestId)).isEmpty();
    }

    @Test
    @DisplayName("수락이 커밋되면 요청이 사라지고 양방향 친구 관계와 수락 이벤트가 생긴다")
    void accept_CommitsFriendshipAndRemoval_PendingRequest() {
        // given
        actions.sendRequest(high, low);

        // when
        actions.updateStatus(low, high, FriendRequestStatus.ACCEPTED);

        // then
        assertThat(requests.findById(requestId)).isEmpty();
        assertThat(friendships.findByUserId(low)).hasSize(1);
        assertThat(friendships.findByUserId(high)).hasSize(1);
        assertThat(acceptedEvents.events).containsExactly(new FriendRequestAcceptedEvent(high, low, "low"));
    }

    @Test
    @DisplayName("관계 생성과 요청 삭제 뒤 실패하면 두 쓰기가 함께 롤백된다")
    void accept_RollsBackBothWrites_ErrorAfterDelete() {
        // given
        actions.sendRequest(low, high);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("acceptance interrupted after delete");
        }).when(requests).deleteById(requestId);

        // when & then
        assertThatThrownBy(() -> actions.updateStatus(high, low, FriendRequestStatus.ACCEPTED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(friendships.existsFriendshipBetween(low, high)).isFalse();
        assertThat(requests.findById(requestId)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
    }

    @Test
    @DisplayName("관계 확인을 지나쳐도 중복 관계로 수락이 실패하면 요청이 남고 수락 이벤트가 없다")
    void accept_KeepsRequest_DuplicateFriendshipKey() {
        // given
        actions.sendRequest(low, high);
        friendships.create(Friendship.restore(LocalDate.of(2026, 1, 1),
                FriendRecognition.restore(low, "기존", 21.4, null, false, true),
                FriendRecognition.restore(high, null, 21.4, null, false, true)));
        doReturn(false).when(friendshipLocks).existsFriendshipForUpdate(low, high);

        // when & then
        assertThatThrownBy(() -> actions.updateStatus(high, low, FriendRequestStatus.ACCEPTED))
                .isInstanceOf(AlreadyFriendsException.class);
        assertThat(requests.findById(requestId)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
        assertThat(friendships.findBetween(low, high)).get()
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
        }).when(requests).saveRequest(any(FriendRequest.class));

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(outcome(() -> actions.sendRequest(low, high)));
            var second = workers.submit(outcome(() -> actions.sendRequest(high, low)));

            // then
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(FriendRequestResult.class, DuplicateFriendRequestException.class);
        }
        assertThat(requests.findById(requestId)).isPresent();
    }

    @Test
    @DisplayName("요청 삽입 전 사전 검사가 오래돼도 수락한 관계에 새 요청이 남지 않는다")
    void sendRequest_RollsBackResend_ConcurrentAcceptance() throws Exception {
        // given
        CountDownLatch beforeInsert = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        doAnswer(invocation -> {
            FriendRequest pending = invocation.getArgument(0);
            if (pending.getRequesterId().equals(high)) {
                beforeInsert.countDown();
                await(resume);
            }
            return invocation.callRealMethod();
        }).when(requests).saveRequest(any(FriendRequest.class));

        // when
        try (var workers = Executors.newSingleThreadExecutor()) {
            var resend = workers.submit(outcome(() -> actions.sendRequest(high, low)));
            try {
                await(beforeInsert);
                actions.sendRequest(low, high);
                actions.updateStatus(high, low, FriendRequestStatus.ACCEPTED);
            } finally {
                resume.countDown();
            }

            // then
            assertThat(resend.get(10, TimeUnit.SECONDS)).isEqualTo(AlreadyFriendsException.class);
        }
        assertThat(friendships.existsFriendshipBetween(low, high)).isTrue();
        assertThat(requests.findById(requestId)).isEmpty();
    }

    @Test
    @DisplayName("수락이 요청 행을 먼저 잠그면 뒤따른 취소는 수락 결과를 지우지 못한다")
    void cancel_FailsAfterAcceptance_RequestRowLocked() throws Exception {
        // given
        actions.sendRequest(low, high);
        CountDownLatch beforeDelete = new CountDownLatch(1);
        CountDownLatch cancelStarted = new CountDownLatch(1);
        CountDownLatch finishAcceptance = new CountDownLatch(1);
        doAnswer(invocation -> {
            beforeDelete.countDown();
            await(finishAcceptance);
            return invocation.callRealMethod();
        }).when(requests).deleteById(requestId);

        // when
        try (var workers = Executors.newFixedThreadPool(2)) {
            var accept = workers.submit(() -> actions.updateStatus(high, low, FriendRequestStatus.ACCEPTED));
            try {
                await(beforeDelete);
                var cancel = workers.submit(outcome(() -> {
                    cancelStarted.countDown();
                    actions.cancelRequest(high, low);
                    return null;
                }));
                await(cancelStarted);
                finishAcceptance.countDown();
                accept.get(10, TimeUnit.SECONDS);

                // then
                assertThat(cancel.get(10, TimeUnit.SECONDS)).isEqualTo(FriendRequestNotFoundException.class);
            } finally {
                finishAcceptance.countDown();
            }
        }
        assertThat(friendships.existsFriendshipBetween(low, high)).isTrue();
        assertThat(requests.findById(requestId)).isEmpty();
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
