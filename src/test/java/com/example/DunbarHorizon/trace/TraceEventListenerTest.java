package com.example.DunbarHorizon.trace;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.trace.application.TraceEventListener;
import com.example.DunbarHorizon.trace.domain.event.TraceRevealedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceEventListenerTest {

    @InjectMocks
    private TraceEventListener traceEventListener;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserQueryUseCase userQueryUseCase;

    @Test
    @DisplayName("정체 공개 이벤트가 발생하면 수신자별로 상대방 프로필이 담긴 알림 이벤트를 각각 발행한다")
    void handleTraceRevealed_PublishesTwoEventsWithOpponentProfile() {
        // given
        TraceRevealedEvent event = new TraceRevealedEvent(1L, 2L);
        when(userQueryUseCase.getUserProfiles(List.of(1L, 2L))).thenReturn(List.of(
                new UserProfileInfo(1L, "nickA", "imgA"),
                new UserProfileInfo(2L, "nickB", "imgB")
        ));

        // when
        traceEventListener.handleTraceRevealed(event);

        // then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<NotificationEvent> published = captor.getAllValues();

        NotificationEvent toMinUser = published.stream()
                .filter(e -> e.receiverIds().contains(1L))
                .findFirst().orElseThrow();
        NotificationEvent toMaxUser = published.stream()
                .filter(e -> e.receiverIds().contains(2L))
                .findFirst().orElseThrow();

        assertThat(toMinUser.type()).isEqualTo(NotificationType.TRACE_REVEALED);
        assertThat(toMinUser.content()).isEqualTo("nickB님과 서로 통했습니다! 방문해서 인사를 건네보세요");
        assertThat(toMinUser.metadata()).containsEntry("senderUserId", 2L);
        assertThat(toMinUser.metadata()).containsEntry("senderNickname", "nickB");
        assertThat(toMinUser.metadata()).containsEntry("senderProfileImageUrl", "imgB");

        assertThat(toMaxUser.type()).isEqualTo(NotificationType.TRACE_REVEALED);
        assertThat(toMaxUser.content()).isEqualTo("nickA님과 서로 통했습니다! 방문해서 인사를 건네보세요");
        assertThat(toMaxUser.metadata()).containsEntry("senderUserId", 1L);
        assertThat(toMaxUser.metadata()).containsEntry("senderNickname", "nickA");
        assertThat(toMaxUser.metadata()).containsEntry("senderProfileImageUrl", "imgA");
    }

    @Test
    @DisplayName("프로필 조회 실패 시 알림 이벤트를 발행하지 않는다")
    void handleTraceRevealed_DoesNotPublishWhenProfileNotFound() {
        // given
        TraceRevealedEvent event = new TraceRevealedEvent(1L, 2L);
        when(userQueryUseCase.getUserProfiles(List.of(1L, 2L))).thenReturn(
                List.of(new UserProfileInfo(1L, "nickA", "imgA"))
        );

        // when
        traceEventListener.handleTraceRevealed(event);

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }
}
