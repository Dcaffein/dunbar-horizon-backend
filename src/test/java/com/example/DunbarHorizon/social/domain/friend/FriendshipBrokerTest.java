package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestNotAcceptedException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FriendshipBrokerTest {

    @InjectMocks
    private FriendshipBroker friendshipBroker;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Nested
    @DisplayName("propose (친구 요청 제안) 테스트")
    class ProposeTest {

        @Test
        @DisplayName("이미 친구 관계인 경우 예외가 발생한다")
        void propose_AlreadyFriends_Fail() {
            // given
            UserReference requester = createMockUser(1L);
            UserReference receiver = createMockUser(2L);
            given(friendshipRepository.existsFriendshipBetween(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendshipBroker.propose(requester, receiver))
                    .isInstanceOf(AlreadyFriendsException.class);
        }

        @Test
        @DisplayName("이미 대기 중인 요청이 있으면 예외가 발생한다")
        void propose_DuplicateRequest_Fail() {
            // given
            UserReference requester = createMockUser(1L);
            UserReference receiver = createMockUser(2L);
            given(friendshipRepository.existsFriendshipBetween(1L, 2L)).willReturn(false);
            given(friendRequestRepository.existsRequestBetween(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendshipBroker.propose(requester, receiver))
                    .isInstanceOf(DuplicateFriendRequestException.class);
        }

        @Test
        @DisplayName("정상적인 조건에서 FriendRequest가 생성된다")
        void propose_Success() {
            // given
            UserReference requester = createMockUser(1L);
            UserReference receiver = createMockUser(2L);
            given(friendshipRepository.existsFriendshipBetween(1L, 2L)).willReturn(false);
            given(friendRequestRepository.existsRequestBetween(1L, 2L)).willReturn(false);

            // when
            FriendRequest result = friendshipBroker.propose(requester, receiver);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRequester().getId()).isEqualTo(1L);
            assertThat(result.getReceiver().getId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("createFrom (친구 관계 확정) 테스트")
    class CreateFromTest {

        @Test
        @DisplayName("수락되지 않은 요청은 Friendship으로 바꿀 수 없다")
        void createFrom_NotAccepted_Fail() {
            // given
            FriendRequest request = mock(FriendRequest.class);
            given(request.isAccepted()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> friendshipBroker.createFrom(request))
                    .isInstanceOf(FriendRequestNotAcceptedException.class);
        }

        @Test
        @DisplayName("이미 친구 관계인 경우 예외가 발생한다")
        void createFrom_AlreadyFriends_Fail() {
            // given
            UserReference requester = createMockUser(1L);
            UserReference receiver = createMockUser(2L);
            FriendRequest request = mock(FriendRequest.class);

            given(request.isAccepted()).willReturn(true);
            given(request.getRequester()).willReturn(requester);
            given(request.getReceiver()).willReturn(receiver);
            given(friendshipRepository.existsFriendshipBetween(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> friendshipBroker.createFrom(request))
                    .isInstanceOf(AlreadyFriendsException.class);
        }

        @Test
        @DisplayName("수락된 요청을 통해 정상적으로 Friendship이 생성된다")
        void createFrom_Success() {
            // given
            UserReference requester = createMockUser(1L);
            UserReference receiver = createMockUser(2L);
            FriendRequest request = mock(FriendRequest.class);

            given(request.isAccepted()).willReturn(true);
            given(request.getRequester()).willReturn(requester);
            given(request.getReceiver()).willReturn(receiver);
            given(friendshipRepository.existsFriendshipBetween(1L, 2L)).willReturn(false);

            // when
            Friendship result = friendshipBroker.createFrom(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("1_2");
        }
    }

    private UserReference createMockUser(Long id) {
        UserReference user = mock(UserReference.class);
        given(user.getId()).willReturn(id);
        return user;
    }
}