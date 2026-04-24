package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.FriendRequestCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendRequestCleanupScheduler {

    private final FriendRequestCleanupService cleanupService;

    @Scheduled(cron = "0 0 4 * * *")
    public void runCleanup() {
        cleanupService.deleteExpiredHiddenRequests();
    }
}
