package com.example.GooRoomBe.social.friend.application;

import com.example.GooRoomBe.social.friend.domain.FriendRequest;
import com.example.GooRoomBe.social.friend.domain.Friendship;
import com.example.GooRoomBe.social.friend.domain.FriendshipPort;
import com.example.GooRoomBe.social.friend.domain.event.FriendRequestAcceptedEvent;
import com.example.GooRoomBe.social.friend.domain.factory.FriendshipFactory;
import com.example.GooRoomBe.social.friend.exception.FriendRequestNotFoundException;
import com.example.GooRoomBe.social.friend.infrastructure.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendshipEventListenerTest {

    @Mock private FriendshipFactory friendshipFactory;
    @Mock private FriendshipPort friendshipPort;
    @Mock private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private FriendshipEventListener listener;

    @Test
    @DisplayName("요청 수락 이벤트 수신 시: 친구 요청을 조회하여 Friendship을 생성하고 저장한다")
    void handleFriendRequestAccepted_Success() {
        // Given
        String requestId = "req-123";
        String requesterId = "userA";
        String receiverId = "userB";

        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent(requestId, requesterId, receiverId);

        FriendRequest mockRequest = mock(FriendRequest.class);
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(mockRequest));

        Friendship mockFriendship = mock(Friendship.class);
        given(friendshipFactory.createFromRequest(mockRequest)).willReturn(mockFriendship);

        // When
        listener.handleFriendRequestAccepted(event);

        // Then
        verify(friendshipPort).save(mockFriendship);
    }

    @Test
    @DisplayName("요청 수락 이벤트 수신 시: 해당 요청 ID가 없으면 예외를 던진다")
    void handleFriendRequestAccepted_NotFound() {
        // Given
        String requestId = "unknown-req";

        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent(requestId, "dummy1", "dummy2");

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> listener.handleFriendRequestAccepted(event))
                .isInstanceOf(FriendRequestNotFoundException.class);
    }
}