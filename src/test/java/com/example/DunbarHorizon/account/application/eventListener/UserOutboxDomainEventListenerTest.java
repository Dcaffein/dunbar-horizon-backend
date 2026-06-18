package com.example.DunbarHorizon.account.application.eventListener;

import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.account.domain.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class UserOutboxDomainEventListenerTest {

    private UserEventOutboxRepository outboxRepository;
    private ApplicationEventPublisher eventPublisher;
    private ProfileImageStoragePort profileImageStoragePort;
    private UserOutboxDomainEventListener listener;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(UserEventOutboxRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        profileImageStoragePort = mock(ProfileImageStoragePort.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new UserOutboxDomainEventListener(outboxRepository, eventPublisher, objectMapper, profileImageStoragePort);
    }

    @Test
    void 유저_활성화_이벤트_수신_시_PENDING_상태의_Outbox_레코드가_저장된다() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", "https://img.url");
        ArgumentCaptor<UserEventOutbox> captor = ArgumentCaptor.forClass(UserEventOutbox.class);
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onUserActivated(event);

        // then
        verify(outboxRepository).save(captor.capture());
        UserEventOutbox saved = captor.getValue();
        assertThat(saved.getAggregateId()).isEqualTo(1L);
        assertThat(saved.getEventType()).isEqualTo(UserOutboxEventType.ACTIVATE);
        assertThat(saved.getStatus()).isEqualTo(UserOutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void 유저_비활성화_이벤트_수신_시_PENDING_상태의_Outbox_레코드가_저장된다() {
        // given
        UserDeactivatedEvent event = new UserDeactivatedEvent(2L);
        ArgumentCaptor<UserEventOutbox> captor = ArgumentCaptor.forClass(UserEventOutbox.class);
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onUserDeactivated(event);

        // then
        verify(outboxRepository).save(captor.capture());
        UserEventOutbox saved = captor.getValue();
        assertThat(saved.getAggregateId()).isEqualTo(2L);
        assertThat(saved.getEventType()).isEqualTo(UserOutboxEventType.DEACTIVATE);
        assertThat(saved.getStatus()).isEqualTo(UserOutboxStatus.PENDING);
    }

    @Test
    void 유저_활성화_이벤트_수신_시_UserSyncIntegrationEvent가_즉시_발행된다() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", "https://img.url");
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onUserActivated(event);

        // then
        ArgumentCaptor<UserSyncIntegrationEvent> captor = ArgumentCaptor.forClass(UserSyncIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        UserSyncIntegrationEvent published = captor.getValue();
        assertThat(published.userId()).isEqualTo(1L);
        assertThat(published.eventType()).isEqualTo(UserOutboxEventType.ACTIVATE);
        assertThat(published.nickname()).isEqualTo("testUser");
        assertThat(published.outboxId()).isNotNull();
    }
}
