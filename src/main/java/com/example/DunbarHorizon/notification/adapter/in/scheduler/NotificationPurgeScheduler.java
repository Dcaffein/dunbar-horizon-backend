package com.example.DunbarHorizon.notification.adapter.in.scheduler;

import com.example.DunbarHorizon.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPurgeScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 30 3 * * *")
    public void runPurge() {
        notificationService.purgeExpiredNotifications();
    }
}
