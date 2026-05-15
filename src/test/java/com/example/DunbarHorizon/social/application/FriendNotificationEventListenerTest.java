package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.social.application.eventListener.FriendNotificationEventListener;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendNotificationEventListenerTest {

    @InjectMocks
    private FriendNotificationEventListener friendNotificationEventListener;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("친구 수락 이벤트 수신 시 요청자에게 올바른 내용의 NotificationEvent를 발행한다")
    void onFriendRequestAccepted_올바른_NotificationEvent_발행() {
        // given
        Long requesterId = 1L;
        Long receiverId = 2L;
        String receiverNickname = "수신자닉네임";
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent("reqId", requesterId, receiverId, receiverNickname);

        // when
        friendNotificationEventListener.onFriendRequestAccepted(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent published = captor.getValue();
        assertThat(published.receiverIds()).containsExactly(requesterId);
        assertThat(published.title()).isEqualTo("친구 수락");
        assertThat(published.content()).isEqualTo(receiverNickname + "님과 이제 친구입니다.");
        assertThat(published.type()).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPT);
        assertThat(published.metadata()).containsEntry("friendId", receiverId);
        assertThat(published.metadata()).containsEntry("friendName", receiverNickname);
    }
}
