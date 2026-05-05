package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendshipDecayService {
    private final FriendshipRepository friendshipRepository;
    private final FriendshipDecayPolicy decayPolicy;

    @Neo4jTransactional
    public void processDecay() {
        double rate = decayPolicy.getDecayRate();
        double minThreshold = decayPolicy.getMinThreshold();
        LocalDateTime thresholdTime = decayPolicy.getDecayThresholdTime();
        friendshipRepository.applyDecay(rate, minThreshold, thresholdTime);
    }
}