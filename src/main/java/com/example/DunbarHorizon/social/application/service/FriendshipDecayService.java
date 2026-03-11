package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendshipDecayService {
    private final FriendshipRepository friendshipRepository;
    private final FriendshipDecayPolicy decayPolicy;

    @Transactional
    public void processDecay() {
        double rate = decayPolicy.getDecayRate();
        double minThreshold = decayPolicy.getMinThreshold();
        LocalDateTime thresholdTime = decayPolicy.getDecayThresholdTime();
        friendshipRepository.applyDecay(rate, minThreshold, thresholdTime);
    }
}