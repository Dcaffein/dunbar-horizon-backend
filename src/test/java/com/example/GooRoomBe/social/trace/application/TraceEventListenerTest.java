package com.example.GooRoomBe.social.trace.application;

import com.example.GooRoomBe.global.event.NotificationEvent;
import com.example.GooRoomBe.global.event.NotificationType;
import com.example.GooRoomBe.social.trace.domain.event.TraceRevealedEvent;
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
class TraceEventListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TraceEventListener traceEventListener;

    @Test
    @DisplayName("TraceRevealedEvent를 수신하면 닉네임이 포함된 NotificationEvent를 발행한다")
    void handleTraceRevealed_ShouldPublishNotification() {
        // Given
        String visitorNick = "구름이";
        TraceRevealedEvent event = new TraceRevealedEvent("visitorId", visitorNick, "targetId");

        // When
        traceEventListener.handleTraceRevealed(event);

        // Then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent publishedEvent = captor.getValue();

        assertThat(publishedEvent.receiverId()).isEqualTo("targetId"); // 받는 사람 = Target
        assertThat(publishedEvent.type()).isEqualTo(NotificationType.TRACE_REVEALED);
        // 메시지에 닉네임이 포함되어야 함
        assertThat(publishedEvent.content()).contains(visitorNick);
        assertThat(publishedEvent.relatedUrl()).contains("visitorId");
    }
}