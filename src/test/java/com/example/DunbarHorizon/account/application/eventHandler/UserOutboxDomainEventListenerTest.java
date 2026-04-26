package com.example.DunbarHorizon.account.application.eventHandler;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class UserOutboxDomainEventListenerTest {

    private UserEventOutboxRepository outboxRepository;
    private ApplicationEventPublisher eventPublisher;
    private UserOutboxDomainEventListener listener;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(UserEventOutboxRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new UserOutboxDomainEventListener(outboxRepository, eventPublisher, objectMapper);
        TransactionSynchronizationManager.initSynchronization();
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
    void AFTER_COMMIT_콜백이_TransactionSynchronization에_등록된다() {
        // given
        UserActivatedEvent event = new UserActivatedEvent(1L, "testUser", null);
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        listener.onUserActivated(event);

        // then
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @BeforeEach
    void tearDown() {
        // @BeforeEach가 두 번 실행되지 않도록 직접 정리
    }

    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        TransactionSynchronizationManager.clearSynchronization();
    }
}
