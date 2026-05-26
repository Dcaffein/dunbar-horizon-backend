package com.example.DunbarHorizon.account.adapter.in.scheduler;

import com.example.DunbarHorizon.account.application.service.UserOutboxCleanupService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserOutboxCleanupScheduler {

    private final UserOutboxCleanupService cleanupService;

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "userOutboxCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void cleanupProcessedOutbox() {
        cleanupService.cleanupProcessed();
    }
}
