package com.example.DunbarHorizon.social.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendshipDecayService {
    private final IntimacyScoreManager intimacyScoreManager;

    public void processDecay() {
        intimacyScoreManager.enqueueDecay();
    }
}
