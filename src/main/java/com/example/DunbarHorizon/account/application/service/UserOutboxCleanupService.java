package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOutboxCleanupService {

    private static final int RETENTION_DAYS = 7;

    private final UserEventOutboxRepository outboxRepository;

    @Transactional
    public void cleanupProcessed() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        outboxRepository.deleteProcessedOlderThan(threshold);
        log.debug("[UserOutboxCleanup] Deleted processed outbox records older than {} days", RETENTION_DAYS);
    }
}
