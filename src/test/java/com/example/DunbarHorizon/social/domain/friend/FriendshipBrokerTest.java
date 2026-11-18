package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.CannotRequestToSelfException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotAcceptedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendshipBrokerTest {

    private final FriendshipBroker friendshipBroker = new FriendshipBroker();

    @Test
    @DisplayName("이미 친구이면 요청 존재 여부와 관계없이 친구 중복 예외가 발생한다")
    void propose_Fails_ExistingFriendship() {
        assertThatThrownBy(() -> friendshipBroker.propose(1L, 2L, true, true))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    @DisplayName("같은 사용자 쌍의 요청이 존재하면 새 요청을 생성하지 않는다")
    void propose_Fails_ExistingRequest() {
        assertThatThrownBy(() -> friendshipBroker.propose(1L, 2L, false, true))
                .isInstanceOf(DuplicateFriendRequestException.class);
        assertThatThrownBy(() -> friendshipBroker.propose(2L, 1L, false, true))
                .isInstanceOf(DuplicateFriendRequestException.class);
    }

    @Test
    @DisplayName("기존 요청과 관계가 없으면 발신자 방향을 보존해 요청을 생성한다")
    void propose_PreservesDirection_NoExistingRelationship() {
        // when
        FriendRequest request = friendshipBroker.propose(2L, 1L, false, false);

        // then
        assertThat(request.getRequesterId()).isEqualTo(2L);
        assertThat(request.getReceiverId()).isEqualTo(1L);
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("기존 관계가 없어도 자신에게 요청할 수 없다")
    void propose_Fails_SameUser() {
        assertThatThrownBy(() -> friendshipBroker.propose(1L, 1L, false, false))
                .isInstanceOf(CannotRequestToSelfException.class);
    }

    @Test
    @DisplayName("기존 관계가 없고 요청이 대기 중이면 미수락 예외가 발생한다")
    void createFrom_Fails_NotAccepted() {
        // given
        FriendRequest request = new FriendRequest(1L, 2L);

        // when & then
        assertThatThrownBy(() -> friendshipBroker.createFrom(request, false))
                .isInstanceOf(FriendRequestNotAcceptedException.class);
    }

    @ParameterizedTest
    @EnumSource(FriendRequestStatus.class)
    @DisplayName("기존 관계가 있으면 요청 상태와 관계없이 친구 중복 예외가 우선한다")
    void createFrom_Fails_ExistingFriendship(FriendRequestStatus status) {
        // given
        FriendRequest request = new FriendRequest(1L, 2L);
        if (status != FriendRequestStatus.PENDING) {
            request.updateStatus(2L, status);
        }

        // when & then
        assertThatThrownBy(() -> friendshipBroker.createFrom(request, true))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @ParameterizedTest
    @CsvSource({"1, 2", "2, 1"})
    @DisplayName("수락된 요청의 양쪽 참여자를 ID 순서로 정렬해 친구 관계를 생성한다")
    void createFrom_CreatesFriendship_AcceptedRequest(Long requesterId, Long receiverId) {
        // given
        FriendRequest request = FriendTestFactory.createAcceptedRequest(requesterId, receiverId);

        // when
        Friendship friendship = friendshipBroker.createFrom(request, false);

        // then
        assertThat(friendship.getLower().getOwnerId()).isEqualTo(1L);
        assertThat(friendship.getHigher().getOwnerId()).isEqualTo(2L);
        assertThat(friendship.getFriendId(requesterId)).isEqualTo(receiverId);
        assertThat(friendship.getFriendId(receiverId)).isEqualTo(requesterId);
    }
}
