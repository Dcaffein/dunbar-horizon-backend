package com.example.DunbarHorizon.account.adapter.in.scheduler;

import com.example.DunbarHorizon.account.application.service.UserOutboxRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class UserOutboxRetryScheduler {

    private final UserOutboxRetryService retryService;

    @Scheduled(fixedDelay = 300_000)
    public void retryPendingOutboxEvents() {
        log.debug("[UserOutboxRetryScheduler] Scanning pending outbox events");
        retryService.retryPending();
    }
}
