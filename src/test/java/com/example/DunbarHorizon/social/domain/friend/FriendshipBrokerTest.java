package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.CannotRequestToSelfException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotAcceptedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendshipBrokerTest {

    @Test
    @DisplayName("이미 친구이면 요청 존재 여부와 관계없이 친구 중복 예외가 발생한다")
    void propose_Fails_ExistingFriendship() {
        assertThatThrownBy(() -> FriendshipBroker.propose(1L, 2L, true, true))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    @DisplayName("같은 사용자 쌍의 요청이 존재하면 새 요청을 생성하지 않는다")
    void propose_Fails_ExistingRequest() {
        assertThatThrownBy(() -> FriendshipBroker.propose(1L, 2L, false, true))
                .isInstanceOf(DuplicateFriendRequestException.class);
        assertThatThrownBy(() -> FriendshipBroker.propose(2L, 1L, false, true))
                .isInstanceOf(DuplicateFriendRequestException.class);
    }

    @Test
    @DisplayName("기존 요청과 관계가 없으면 발신자 방향을 보존해 요청을 생성한다")
    void propose_PreservesDirection_NoExistingRelationship() {
        // when
        FriendRequest request = FriendshipBroker.propose(2L, 1L, false, false);

        // then
        assertThat(request.getRequesterId()).isEqualTo(2L);
        assertThat(request.getReceiverId()).isEqualTo(1L);
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("기존 관계가 없어도 자신에게 요청할 수 없다")
    void propose_Fails_SameUser() {
        assertThatThrownBy(() -> FriendshipBroker.propose(1L, 1L, false, false))
                .isInstanceOf(CannotRequestToSelfException.class);
    }

    @Test
    @DisplayName("수락되지 않은 요청은 기존 관계 여부와 무관하게 친구 관계로 바꿀 수 없다")
    void createFrom_Fails_NotAccepted() {
        // given
        FriendRequest request = new FriendRequest(1L, 2L);

        // when & then
        assertThatThrownBy(() -> FriendshipBroker.createFrom(request, false))
                .isInstanceOf(FriendRequestNotAcceptedException.class);
        assertThatThrownBy(() -> FriendshipBroker.createFrom(request, true))
                .isInstanceOf(FriendRequestNotAcceptedException.class);
    }

    @Test
    @DisplayName("수락된 요청이어도 기존 친구 관계가 있으면 관계를 생성하지 않는다")
    void createFrom_Fails_ExistingFriendship() {
        // given
        FriendRequest request = FriendTestFactory.createAcceptedRequest(1L, 2L);

        // when & then
        assertThatThrownBy(() -> FriendshipBroker.createFrom(request, true))
                .isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    @DisplayName("수락된 요청에서 두 사용자의 친구 관계를 생성한다")
    void createFrom_CreatesFriendship_AcceptedRequest() {
        // given
        FriendRequest request = FriendTestFactory.createAcceptedRequest(2L, 1L);

        // when
        Friendship friendship = FriendshipBroker.createFrom(request, false);

        // then
        assertThat(friendship.getUserIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(friendship.getFriendId(2L)).isEqualTo(1L);
    }
}
