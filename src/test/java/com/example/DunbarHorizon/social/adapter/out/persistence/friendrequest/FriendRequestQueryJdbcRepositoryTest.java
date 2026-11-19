package com.example.DunbarHorizon.social.adapter.out.persistence.friendrequest;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestView;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequest;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friendship.FriendTestFactory;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import(FriendRequestQueryJdbcRepository.class)
class FriendRequestQueryJdbcRepositoryTest {

    @Autowired private FriendRequestQueryJdbcRepository friendRequestQueryJdbcRepository;
    @Autowired private TestEntityManager testEntityManager;

    private User requester;
    private User receiver;

    @BeforeEach
    void setUp() {
        requester = createUser("requester");
        receiver = createUser("receiver");
    }

    @Test
    @DisplayName("보낸 목록과 받은 목록에 양쪽 닉네임과 저장된 상태를 담는다")
    void findSentAndReceived_ShowsBothNicknamesAndStoredStatus() {
        // given
        FriendRequest request = FriendTestFactory.createRequest(requester.getId(), receiver.getId());
        request.updateStatus(receiver.getId(), FriendRequestStatus.HIDDEN);
        testEntityManager.persistAndFlush(request);
        testEntityManager.clear();

        // when
        var sent = friendRequestQueryJdbcRepository.findSent(requester.getId());
        var receivedHidden = friendRequestQueryJdbcRepository.findReceived(receiver.getId(), FriendRequestStatus.HIDDEN);

        // then
        assertThat(sent).singleElement().satisfies(view -> {
            assertThat(view.requesterId()).isEqualTo(requester.getId());
            assertThat(view.requesterNickname()).isEqualTo("requester");
            assertThat(view.receiverId()).isEqualTo(receiver.getId());
            assertThat(view.receiverNickname()).isEqualTo("receiver");
            assertThat(view.status()).isEqualTo(FriendRequestStatus.HIDDEN);
            assertThat(view.createdAt()).isNotNull();
        });
        assertThat(receivedHidden).containsExactlyElementsOf(sent);
        assertThat(friendRequestQueryJdbcRepository.findSentTo(requester.getId(), receiver.getId())).contains(sent.getFirst());
        assertThat(friendRequestQueryJdbcRepository.findSentTo(receiver.getId(), requester.getId())).isEmpty();
        assertThat(friendRequestQueryJdbcRepository.findReceived(receiver.getId(), FriendRequestStatus.PENDING)).isEmpty();
        assertThat(friendRequestQueryJdbcRepository.findSent(receiver.getId())).isEmpty();
    }

    @Test
    @DisplayName("요청자나 수신자가 비활성이면 두 목록에서 보이지 않는다")
    void findSentAndReceived_HideRequest_InactiveUser() {
        // given
        testEntityManager.persist(FriendTestFactory.createRequest(requester.getId(), receiver.getId()));
        receiver.deactivate();
        testEntityManager.flush();
        testEntityManager.clear();

        // when & then
        assertThat(friendRequestQueryJdbcRepository.findSent(requester.getId())).isEmpty();
        assertThat(friendRequestQueryJdbcRepository.findReceived(receiver.getId(), FriendRequestStatus.PENDING)).isEmpty();
        assertThat(friendRequestQueryJdbcRepository.findSentTo(requester.getId(), receiver.getId())).hasValueSatisfying(view -> {
            assertThat(view.requesterNickname()).isEqualTo("requester");
            assertThat(view.receiverNickname()).isNull();
        });
    }

    private User createUser(String nickname) {
        return testEntityManager.persistAndFlush(User.builder()
                .email(nickname + "@jdbc-request-views.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }
}
