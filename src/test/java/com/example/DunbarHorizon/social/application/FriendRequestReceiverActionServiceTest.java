package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.service.FriendRequestReceiverActionService;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
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
    private FriendshipBroker friendshipBroker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("수신자가 요청을 수락하면 Friendship이 수립되고 요청은 DELETED 상태로 완료된다")
    void acceptFriendRequest_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long receiverId = 2L;
        SocialUser req = mock(SocialUser.class);
        SocialUser res = mock(SocialUser.class);
        given(res.getId()).willReturn(receiverId);
        given(res.getNickname()).willReturn("수신자");

        FriendRequest request = FriendTestFactory.createRequest(req, res);
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        receiverService.acceptRequest(requestId, receiverId);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.DELETED);

        verify(friendshipBroker).establish(request);
        verify(eventPublisher).publishEvent(any(FriendRequestAcceptedEvent.class));
        verify(friendRequestRepository).saveRequest(request);
    }

    @Test
    @DisplayName("수신자가 요청을 숨기면 상태가 HIDDEN으로 변경된다")
    void hideFriendRequest_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long receiverId = 2L;
        SocialUser res = mock(SocialUser.class);
        given(res.getId()).willReturn(receiverId);

        FriendRequest request = FriendTestFactory.createRequest(mock(SocialUser.class), res);
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        receiverService.hideRequest(requestId, receiverId);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.HIDDEN);
        verify(friendRequestRepository).saveRequest(request);
    }
}