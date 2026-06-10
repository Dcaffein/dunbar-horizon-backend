package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.service.FriendRequestReceiverActionService;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.event.FriendshipCreatedEvent;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequestReceiverActionServiceTest {

    @InjectMocks
    private FriendRequestReceiverActionService receiverService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipBroker friendshipBroker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("수신자가 요청을 수락하면 Friendship이 저장되고 요청은 삭제되며 이벤트가 발행된다")
    void acceptFriendRequest_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long receiverId = 2L;
        SocialUser requester = mock(SocialUser.class);
        SocialUser receiver = mock(SocialUser.class);
        given(requester.getId()).willReturn(1L);
        given(receiver.getId()).willReturn(receiverId);
        given(receiver.getNickname()).willReturn("수신자");

        FriendRequest request = FriendTestFactory.createRequest(requester, receiver);
        Friendship mockFriendship = mock(Friendship.class);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(friendshipBroker.createFrom(request)).willReturn(mockFriendship);

        // when
        receiverService.acceptRequest(requestId, receiverId);

        // then
        verify(friendshipBroker).createFrom(request);
        verify(friendshipRepository).save(mockFriendship);
        verify(friendRequestRepository).deleteById(requestId);
        verify(eventPublisher).publishEvent(any(FriendshipCreatedEvent.class));
        verify(eventPublisher).publishEvent(any(FriendRequestAcceptedEvent.class));
    }

    @Test
    @DisplayName("숨김 처리된 요청도 수락이 가능하다")
    void acceptFriendRequest_WhenHidden_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long receiverId = 2L;
        SocialUser requester = mock(SocialUser.class);
        SocialUser receiver = mock(SocialUser.class);
        given(requester.getId()).willReturn(1L);
        given(receiver.getId()).willReturn(receiverId);
        given(receiver.getNickname()).willReturn("수신자");

        FriendRequest request = FriendTestFactory.createRequest(requester, receiver);
        request.hide(receiverId);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));
        given(friendshipBroker.createFrom(request)).willReturn(mock(Friendship.class));

        // when
        receiverService.acceptRequest(requestId, receiverId);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(friendshipBroker).createFrom(request);
        verify(friendRequestRepository).deleteById(requestId);
    }

    @Test
    @DisplayName("수신자가 요청을 숨기면 상태가 HIDDEN으로 변경된다")
    void hideFriendRequest_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long receiverId = 2L;
        SocialUser receiver = mock(SocialUser.class);
        given(receiver.getId()).willReturn(receiverId);

        FriendRequest request = FriendTestFactory.createRequest(mock(SocialUser.class), receiver);
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        receiverService.hideRequest(requestId, receiverId);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.HIDDEN);
        verify(friendRequestRepository).saveRequest(request);
    }
}
