package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
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
class FriendshipNotificationEventListenerTest {

    @InjectMocks
    private FriendshipNotificationEventListener listener;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("친구 요청 수락 이벤트 수신 시 요청자에게 NotificationEvent를 발행한다")
    void onFriendRequestAccepted_PublishesNotificationToRequester() {
        // given
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent(1L, 2L, "수락자닉네임");

        // when
        listener.onFriendRequestAccepted(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent published = captor.getValue();
        assertThat(published.receiverIds()).containsExactly(1L);
        assertThat(published.title()).isEqualTo("친구 수락");
        assertThat(published.content()).isEqualTo("수락자닉네임님과 이제 친구입니다.");
        assertThat(published.type()).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPT);
        assertThat(published.metadata()).containsEntry("friendId", 2L);
        assertThat(published.metadata()).containsEntry("friendName", "수락자닉네임");
    }
}
