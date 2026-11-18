package com.example.DunbarHorizon.social.domain.friendship;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipArchiveThreshold {

    private final InterestScoreDecayParameters decayParameters;

    // normalize(MIN_RAW_THRESHOLD) = 1.0 / (1.0 + 50.0) ≈ 0.0196
    public double archiveThreshold() {
        return FriendRecognition.normalize(decayParameters.getMinThreshold());
    }
}
