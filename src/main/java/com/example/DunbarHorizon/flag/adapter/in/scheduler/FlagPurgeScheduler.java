package com.example.DunbarHorizon.flag.adapter.in.scheduler;

import com.example.DunbarHorizon.flag.application.service.flag.FlagPurgeService;
import com.example.DunbarHorizon.flag.application.service.invitation.FlagInvitationCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagPurgeScheduler {

    private final FlagPurgeService purgeService;
    private final FlagInvitationCleanupService invitationCleanupService;

    @Scheduled(cron = "0 0 3 * * *")
    public void runPurge() {
        purgeService.purgeExpiredFlags();
        invitationCleanupService.purgeDeadInvitations();
    }
}