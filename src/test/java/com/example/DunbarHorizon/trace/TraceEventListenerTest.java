package com.example.DunbarHorizon.trace;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.trace.application.TraceEventListener;
import com.example.DunbarHorizon.trace.domain.event.TraceRevealedEvent;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceEventListenerTest {

    @InjectMocks
    private TraceEventListener traceEventListener;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("정체 공개 이벤트가 발생하면 알림 이벤트를 발행한다")
    void handleTraceRevealed_Success() {
        // given
        TraceRevealedEvent event = new TraceRevealedEvent(1L, 2L);

        // when
        traceEventListener.handleTraceRevealed(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent publishedEvent = captor.getValue();

        // [수정] receiverId() -> receiverIds() 리스트 검증
        assertThat(publishedEvent.receiverIds()).contains(2L);
        assertThat(publishedEvent.type()).isEqualTo(NotificationType.TRACE_REVEALED);
        assertThat(publishedEvent.title()).contains("서로간 잦은 방문");
    }
}