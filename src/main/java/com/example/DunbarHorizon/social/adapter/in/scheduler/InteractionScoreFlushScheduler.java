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

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "interactionScoreFlush", lockAtMostFor = "PT30S", lockAtLeastFor = "PT5S")
    public void flush() {
        flushService.flush();
    }
}
