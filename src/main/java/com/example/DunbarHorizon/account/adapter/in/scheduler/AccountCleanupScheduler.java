package com.example.DunbarHorizon.account.adapter.in.scheduler;

import com.example.DunbarHorizon.account.application.port.in.AccountCleanupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountCleanupScheduler {

    private final AccountCleanupUseCase accountCleanupUseCase;

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupGarbageAccounts() {
        accountCleanupUseCase.cleanupExpiredPendingAccounts();
    }
}
