package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.jdbc.FriendshipQueryJdbcRepository;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendResult;
import com.example.DunbarHorizon.social.application.friendrequest.FriendshipNotificationEventListener;
import com.example.DunbarHorizon.social.application.friendrequest.FriendRequestService;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendRequestPersistenceAdapter.class, FriendshipPersistenceAdapter.class,
        FriendRequestService.class, FriendshipBroker.class, FriendRequestQueryJdbcRepository.class,
        FriendshipQueryJdbcRepository.class, FriendshipNotificationEventListener.class,
        FriendRequestLifecycleTest.AcceptedEvents.class})
class FriendRequestLifecycleTest {

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
        acceptedEvents.notifications.clear();
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
        var sent = friendRequestService.sendRequest(high, low);
        assertThat(sent.id()).isEqualTo(low + "_" + high);
        assertThat(sent.requester()).isEqualTo(new FriendResult(high, "high"));
        assertThat(sent.receiver()).isEqualTo(new FriendResult(low, "low"));

        // when
        friendRequestService.updateStatus(low, high, FriendRequestStatus.HIDDEN);

        // then
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).get()
                .extracting(FriendRequest::getStatus).isEqualTo(FriendRequestStatus.HIDDEN);
        assertThat(friendRequestService.getRequests(low, FriendRequestDirection.RECEIVED, FriendRequestStatus.HIDDEN))
                .singleElement().satisfies(request -> {
                    assertThat(request.requester().id()).isEqualTo(high);
                    assertThat(request.receiver().id()).isEqualTo(low);
                });
        friendRequestService.updateStatus(low, high, FriendRequestStatus.PENDING);
        assertThat(friendRequestPersistenceAdapter.findBetween(high, low)).get()
                .extracting(FriendRequest::getStatus).isEqualTo(FriendRequestStatus.PENDING);
        friendRequestService.cancelRequest(high, low);
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
    }

    @Test
    @DisplayName("수락이 커밋된 뒤에만 새 친구를 조회해 요청자에게 알림을 발행한다")
    void accept_CommitsFriendshipAndRemoval_PendingRequest() {
        // given
        friendRequestService.sendRequest(high, low);

        // when
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            friendRequestService.updateStatus(low, high, FriendRequestStatus.ACCEPTED);
            assertThat(acceptedEvents.notifications).isEmpty();
        });

        // then
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isTrue();
        assertThat(acceptedEvents.events).containsExactly(new FriendRequestAcceptedEvent(high, low));
        assertThat(acceptedEvents.notifications).singleElement().satisfies(notification -> {
            assertThat(notification.type()).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPT);
            assertThat(notification.receiverIds()).containsExactly(high);
            assertThat(notification.metadata()).containsEntry("friendId", low).containsEntry("friendName", "low");
        });
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
                .isInstanceOf(IllegalStateException.class);
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
    }

    @Test
    @DisplayName("수락 이벤트 발행 뒤 트랜잭션이 롤백되면 알림을 발행하지 않는다")
    void accept_SkipsNotification_RollbackAfterEventPublished() {
        // given
        friendRequestService.sendRequest(low, high);

        // when
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            friendRequestService.updateStatus(high, low, FriendRequestStatus.ACCEPTED);
            assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isTrue();
            status.setRollbackOnly();
        });

        // then
        assertThat(friendshipPersistenceAdapter.existsBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isPresent();
        assertThat(acceptedEvents.events).isEmpty();
        assertThat(acceptedEvents.notifications).isEmpty();
    }

    static class AcceptedEvents {
        private final ConcurrentLinkedQueue<FriendRequestAcceptedEvent> events = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<NotificationEvent> notifications = new ConcurrentLinkedQueue<>();

        @TransactionalEventListener
        void record(FriendRequestAcceptedEvent event) {
            events.add(event);
        }

        @EventListener
        void recordNotification(NotificationEvent notification) {
            notifications.add(notification);
        }
    }
}
