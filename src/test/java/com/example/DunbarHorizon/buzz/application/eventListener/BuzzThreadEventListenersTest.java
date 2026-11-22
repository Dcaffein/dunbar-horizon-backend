package com.example.DunbarHorizon.buzz.application.eventListener;

import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzThreadCreatedEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzThreadEventListenersTest {

    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendId = 3L;

    @Test
    @DisplayName("스레드를 생성하면 시작자 → 게시자 BUZZ_REPLY를 발행한다")
    void threadCreated_PublishesReplyInteraction() {
        new BuzzInteractionEventListener(applicationEventPublisher)
                .onThreadCreated(new BuzzThreadCreatedEvent("buzz-1", 10L, creatorId, starterId));

        verify(applicationEventPublisher).publishEvent(
                new UserInteractionEvent(starterId, creatorId, InteractionType.BUZZ_REPLY));
    }

    @Test
    @DisplayName("다른 멤버의 답글은 작성자 → 시작자 BUZZ_REPLY를 발행한다")
    void commented_PublishesReplyInteractionToStarter() {
        new BuzzInteractionEventListener(applicationEventPublisher)
                .onCommented(new BuzzCommentedEvent("buzz-1", 10L, creatorId, starterId, friendId));

        verify(applicationEventPublisher).publishEvent(
                new UserInteractionEvent(friendId, starterId, InteractionType.BUZZ_REPLY));
    }

    @Test
    @DisplayName("스레드를 생성하면 게시자에게 스레드 정보가 담긴 BUZZ_RESPONSE 알림을 보낸다")
    void threadCreated_NotifiesCreator() {
        new BuzzNotificationDispatcher(applicationEventPublisher)
                .dispatch(new BuzzThreadCreatedEvent("buzz-1", 10L, creatorId, starterId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().receiverIds()).containsExactly(creatorId);
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.BUZZ_RESPONSE);
        assertThat(captor.getValue().metadata())
                .isEqualTo(Map.of("buzzId", "buzz-1", "threadId", 10L, "authorId", starterId));
    }

    @Test
    @DisplayName("다른 멤버가 답글을 쓰면 게시자와 시작자에게만 알림을 보낸다")
    void commented_NotifiesCreatorAndStarter() {
        new BuzzNotificationDispatcher(applicationEventPublisher)
                .dispatch(new BuzzCommentedEvent("buzz-1", 10L, creatorId, starterId, friendId));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().receiverIds()).containsExactly(creatorId, starterId);
    }
}
