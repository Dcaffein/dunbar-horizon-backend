package com.example.DunbarHorizon.buzz.adapter.in.scheduler;

import com.example.DunbarHorizon.buzz.application.service.BuzzPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BuzzPurgeScheduler {

    private final BuzzPurgeService buzzPurgeService;

    @Scheduled(cron = "0 0 * * * *")
    public void runPurge() {
        buzzPurgeService.purgeExpiredAndDeletedBuzzes(LocalDateTime.now());
    }
}
