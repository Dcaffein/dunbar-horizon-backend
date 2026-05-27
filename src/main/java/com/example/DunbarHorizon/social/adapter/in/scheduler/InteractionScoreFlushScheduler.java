package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.InteractionScoreFlushService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InteractionScoreFlushScheduler {

    private final InteractionScoreFlushService flushService;

    @Scheduled(fixedDelay = 300_000)
    @SchedulerLock(name = "interactionScoreFlush", lockAtMostFor = "PT10M", lockAtLeastFor = "PT4M30S")
    public void flush() {
        flushService.flush();
    }
}
