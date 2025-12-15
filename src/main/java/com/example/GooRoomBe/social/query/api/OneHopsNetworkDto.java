package com.example.GooRoomBe.social.query.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record OneHopsNetworkDto(
        String friendId,
        String friendName,
        String friendAlias,
        Double InterestScore,
        Set<String> mutualFriendIds
) {
    private static final double MAX_THRESHOLD = 500.0;

    @JsonProperty("InterestScore")
    public double getInterestScore() {
        if (InterestScore == null || InterestScore <= 0) {
            return 0.0;
        }

        double logScore = Math.log10(InterestScore + 1);
        double logMax = Math.log10(MAX_THRESHOLD + 1);

        return Math.min(1.0, logScore / logMax);
    }
}