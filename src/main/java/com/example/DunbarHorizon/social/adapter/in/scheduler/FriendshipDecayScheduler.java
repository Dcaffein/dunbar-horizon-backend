package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.MySqlFriendshipScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipDecayScheduler {

    private final MySqlFriendshipScoreService friendshipScores;

    @Scheduled(cron = "${friendship.decay.cron:-}")
    public void runDecay() {
        friendshipScores.processDecay();
    }
}
