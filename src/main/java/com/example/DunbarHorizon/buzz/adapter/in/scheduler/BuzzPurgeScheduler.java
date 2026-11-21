package com.example.DunbarHorizon.buzz.adapter.in.scheduler;

import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BuzzPurgeScheduler {

    private final BuzzService buzzService;

    @Scheduled(cron = "0 0 * * * *")
    public void runPurge() {
        buzzService.purgeExpiredBuzzes(LocalDateTime.now());
    }
}
