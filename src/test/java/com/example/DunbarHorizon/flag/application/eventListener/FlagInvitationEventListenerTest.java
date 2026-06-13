package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.invitation.event.FlagInvitationSentEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
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
class FlagInvitationEventListenerTest {

    @InjectMocks private FlagInvitationEventListener listener;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("일반 초대(isEncore=false)면 '플래그 초대' 제목과 '수락 여부' 본문을 발행한다")
    void handle_일반초대_메시지_검증() {
        // given
        FlagInvitationSentEvent event = new FlagInvitationSentEvent(1L, 10L, 100L, "같이 밥 먹어요", false);

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent notification = captor.getValue();
        assertThat(notification.title()).isEqualTo("플래그 초대");
        assertThat(notification.content()).contains("수락 여부");
        assertThat(notification.content()).contains("같이 밥 먹어요");
    }

    @Test
    @DisplayName("앵콜 초대(isEncore=true)면 '앵콜 초대' 제목과 '앵콜 모임' 본문을 발행한다")
    void handle_앵콜초대_메시지_검증() {
        // given
        FlagInvitationSentEvent event = new FlagInvitationSentEvent(2L, 20L, 200L, "치맥 2탄", true);

        // when
        listener.handle(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent notification = captor.getValue();
        assertThat(notification.title()).isEqualTo("앵콜 초대");
        assertThat(notification.content()).contains("앵콜 모임");
        assertThat(notification.content()).contains("치맥 2탄");
    }
}
