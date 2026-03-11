package com.example.DunbarHorizon.flag.adapter.in;

import com.example.DunbarHorizon.flag.application.service.flag.FlagHardPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagSweepingScheduler {

    private final FlagHardPurgeService hardPurgeService;

    @Scheduled(cron = "0 0 3 * * *")
    public void runSweeping() {
        hardPurgeService.sweepExpiredData();
    }
}