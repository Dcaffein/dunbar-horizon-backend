package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.account.domain.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class UserOutboxRetryServiceTest {

    private UserEventOutboxRepository outboxRepository;
    private ApplicationEventPublisher eventPublisher;
    private UserOutboxRetryService retryService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(UserEventOutboxRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        retryService = new UserOutboxRetryService(outboxRepository, eventPublisher, objectMapper);
    }

    @Test
    void 재시도_횟수가_5회_미만인_경우_retry_count를_증가시키고_Integration_Event를_발행한다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(1L, UserOutboxEventType.ACTIVATE, "{}");
        given(outboxRepository.findPendingOlderThan(any(LocalDateTime.class))).willReturn(List.of(outbox));
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        retryService.retryPending();

        // then
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        verify(eventPublisher).publishEvent(any(UserSyncIntegrationEvent.class));
    }

    @Test
    void 재시도_횟수가_5회를_초과하면_FAILED_상태로_변경하고_Event를_발행하지_않는다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(2L, UserOutboxEventType.ACTIVATE, "{}");
        for (int i = 0; i < 5; i++) outbox.incrementRetry();
        given(outboxRepository.findPendingOlderThan(any(LocalDateTime.class))).willReturn(List.of(outbox));
        given(outboxRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        retryService.retryPending();

        // then
        assertThat(outbox.getStatus()).isEqualTo(UserOutboxStatus.FAILED);
        verify(eventPublisher, never()).publishEvent(any(UserSyncIntegrationEvent.class));
    }

}
