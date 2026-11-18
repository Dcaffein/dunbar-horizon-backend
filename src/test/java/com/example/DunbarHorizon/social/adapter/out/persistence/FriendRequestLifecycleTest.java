package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendRequestViewJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.UserProfileJdbcRepository;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.service.FriendRequestService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({FriendRequestPersistenceAdapter.class, FriendshipJdbcRepository.class, FriendshipPersistenceAdapter.class,
        FriendRequestService.class, FriendshipBroker.class, FriendRequestViewJdbcRepository.class,
        UserProfileJdbcRepository.class,
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
    @DisplayName("수락이 커밋되면 요청이 사라지고 양방향 친구 관계와 수락 이벤트가 생긴다")
    void accept_CommitsFriendshipAndRemoval_PendingRequest() {
        // given
        friendRequestService.sendRequest(high, low);

        // when
        friendRequestService.updateStatus(low, high, FriendRequestStatus.ACCEPTED);

        // then
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).isEmpty();
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isTrue();
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
                .isInstanceOf(IllegalStateException.class);
        assertThat(friendshipPersistenceAdapter.existsFriendshipBetween(low, high)).isFalse();
        assertThat(friendRequestPersistenceAdapter.findBetween(low, high)).get().extracting(FriendRequest::getStatus)
                .isEqualTo(FriendRequestStatus.PENDING);
        assertThat(acceptedEvents.events).isEmpty();
    }

    static class AcceptedEvents {
        private final ConcurrentLinkedQueue<FriendRequestAcceptedEvent> events = new ConcurrentLinkedQueue<>();

        @TransactionalEventListener
        void record(FriendRequestAcceptedEvent event) {
            events.add(event);
        }
    }
}
