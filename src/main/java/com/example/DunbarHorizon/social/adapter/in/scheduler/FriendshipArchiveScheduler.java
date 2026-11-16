package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipArchiveScheduler {

    private final FriendshipCommandService friendships;

    @Scheduled(cron = "0 0 4 * * *")
    public void archive() {
        friendships.archiveFriendships();
    }
}
