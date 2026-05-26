package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserOutboxCleanupServiceTest {

    @InjectMocks
    private UserOutboxCleanupService cleanupService;

    @Mock
    private UserEventOutboxRepository outboxRepository;

    @Test
    @DisplayName("cleanupProcessed 호출 시 7일 이전 기준으로 deleteProcessedOlderThan에 위임한다")
    void cleanupProcessed_7일기준_threshold_전달() {
        // given
        LocalDateTime before = LocalDateTime.now().minusDays(7);

        // when
        cleanupService.cleanupProcessed();

        // then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxRepository).deleteProcessedOlderThan(captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertThat(threshold).isCloseTo(before, within(1, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("cleanupProcessed는 항상 deleteProcessedOlderThan을 호출한다")
    void cleanupProcessed_항상_delete_위임() {
        // when
        cleanupService.cleanupProcessed();

        // then
        verify(outboxRepository).deleteProcessedOlderThan(any(LocalDateTime.class));
    }
}
