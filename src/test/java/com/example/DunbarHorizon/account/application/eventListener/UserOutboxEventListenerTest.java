package com.example.DunbarHorizon.account.application.eventListener;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.account.domain.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class UserOutboxEventListenerTest {

    private UserEventOutboxRepository outboxRepository;
    private ApplicationEventPublisher eventPublisher;
    private UserOutboxEventListener listener;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(UserEventOutboxRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new UserOutboxEventListener(outboxRepository, eventPublisher, objectMapper);
    }

    @Test
    void 유저_활성화_이벤트_수신_시_PENDING_상태의_Outbox_레코드가_저장된다() {
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", "https://img.url");
        ArgumentCaptor<UserEventOutbox> captor = ArgumentCaptor.forClass(UserEventOutbox.class);
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        listener.onUserActivated(event);

        verify(outboxRepository).save(captor.capture());
        UserEventOutbox saved = captor.getValue();
        assertThat(saved.getAggregateId()).isEqualTo(1L);
        assertThat(saved.getEventType()).isEqualTo(UserOutboxEventType.ACTIVATE);
        assertThat(saved.getStatus()).isEqualTo(UserOutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void 유저_비활성화_이벤트_수신_시_PENDING_상태의_Outbox_레코드가_저장된다() {
        UserDeactivatedEvent event = new UserDeactivatedEvent(2L);
        ArgumentCaptor<UserEventOutbox> captor = ArgumentCaptor.forClass(UserEventOutbox.class);
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        listener.onUserDeactivated(event);

        verify(outboxRepository).save(captor.capture());
        UserEventOutbox saved = captor.getValue();
        assertThat(saved.getAggregateId()).isEqualTo(2L);
        assertThat(saved.getEventType()).isEqualTo(UserOutboxEventType.DEACTIVATE);
        assertThat(saved.getStatus()).isEqualTo(UserOutboxStatus.PENDING);
    }

    @Test
    void 유저_활성화_이벤트_수신_시_UserSyncIntegrationEvent가_즉시_발행된다() {
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", "https://img.url");
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        listener.onUserActivated(event);

        ArgumentCaptor<UserSyncIntegrationEvent> captor = ArgumentCaptor.forClass(UserSyncIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        UserSyncIntegrationEvent published = captor.getValue();
        assertThat(published.userId()).isEqualTo(1L);
        assertThat(published.eventType()).isEqualTo(UserOutboxEventType.ACTIVATE);
        assertThat(published.nickname()).isEqualTo("testUser");
        assertThat(published.outboxId()).isNotNull();
    }

    @Test
    void UserSyncCompletedEvent_수신_시_해당_Outbox를_COMPLETED로_변경한다() {
        UserEventOutbox outbox = UserEventOutbox.pending(3L, UserOutboxEventType.ACTIVATE, "{}");
        given(outboxRepository.findById(outbox.getId())).willReturn(Optional.of(outbox));
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        listener.onSyncCompleted(new UserSyncCompletedEvent(outbox.getId()));

        assertThat(outbox.getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_outboxId로_완료_이벤트가_오면_아무_동작도_하지_않는다() {
        given(outboxRepository.findById("unknown-id")).willReturn(Optional.empty());

        listener.onSyncCompleted(new UserSyncCompletedEvent("unknown-id"));

        verify(outboxRepository, never()).save(any());
    }
}
