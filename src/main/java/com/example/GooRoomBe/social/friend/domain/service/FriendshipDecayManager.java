package com.example.GooRoomBe.social.friend.domain.service;

import com.example.GooRoomBe.social.friend.domain.FriendshipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendshipDecayManager {

    private final FriendshipPort friendshipPort;

    private static final double DECAY_RATE = 0.95;
    private static final double MIN_THRESHOLD = 0.1;

    @Transactional
    public void executeDecayPolicy() {
        friendshipPort.applyDecayToAllFriendships(DECAY_RATE, MIN_THRESHOLD);
    }
}