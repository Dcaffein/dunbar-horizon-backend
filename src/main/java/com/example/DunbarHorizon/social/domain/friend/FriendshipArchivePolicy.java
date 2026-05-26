package com.example.DunbarHorizon.social.domain.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendshipArchivePolicy {

    private final FriendshipDecayPolicy decayPolicy;

    // normalize(MIN_RAW_THRESHOLD) = 1.0 / (1.0 + 50.0) ≈ 0.0196
    public double archiveThreshold() {
        return FriendRecognition.normalize(decayPolicy.getMinThreshold());
    }
}
