package com.example.GooRoomBe.social.application;

import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.social.application.eventHandler.FriendEventListener;
import com.example.GooRoomBe.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestNotFoundException;
import com.example.GooRoomBe.social.domain.friend.FriendshipBroker;
import com.example.GooRoomBe.social.domain.friend.FriendRequest;
import com.example.GooRoomBe.social.domain.friend.Friendship;
import com.example.GooRoomBe.social.domain.friend.repository.FriendRequestRepository;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendEventListenerTest {

    @InjectMocks
    private FriendEventListener friendEventListener;
    @Mock
    private FriendshipBroker friendshipBroker;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("친구 수락 이벤트 수신 시 Friendship을 생성 저장하고 알림을 발행한다")
    void onFriendRequestAccepted_Success() {
        // given
        String requestId = "newRequest";
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent(requestId, 1L, 2L, "수신자닉네임");

        FriendRequest mockRequest = mock(FriendRequest.class);
        Friendship mockFriendship = mock(Friendship.class);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(mockRequest));
        given(friendshipBroker.establish(mockRequest)).willReturn(mockFriendship);

        // when
        friendEventListener.onFriendRequestAccepted(event);

        // then
        verify(friendshipRepository).save(mockFriendship);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("요청을 찾을 수 없는 경우 예외를 던진다")
    void onFriendRequestAccepted_NotFound_ThrowsException() {
        // given
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent("newReqeust", 1L, 2L, "닉네임");
        given(friendRequestRepository.findById(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> friendEventListener.onFriendRequestAccepted(event))
                .isInstanceOf(FriendRequestNotFoundException.class);
    }
}