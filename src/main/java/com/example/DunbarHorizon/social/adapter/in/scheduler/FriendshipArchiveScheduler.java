package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.FriendshipArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FriendshipArchiveScheduler {

    private final FriendshipArchiveService friendshipArchiveService;

    @Scheduled(cron = "0 0 4 * * *")
    public void archive() {
        List<String> archivedIds = friendshipArchiveService.archiveFriendships();
        if (!archivedIds.isEmpty()) {
            friendshipArchiveService.deleteArchivedFromNeo4j(archivedIds);
        }
    }
}
