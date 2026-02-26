package com.example.GooRoomBe.social.adapter.in;

import com.example.GooRoomBe.social.application.service.FriendshipDecayService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipDecayScheduler {

    private final FriendshipDecayService friendshipDecayService;

    @Scheduled(cron = "0 0 3 * * *")
    public void runDecay() {
        friendshipDecayService.processDecay();
    }
}
