package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NetworkBetweenOneHopsResult(
        Long friendId,
        String friendName,
        String friendAlias,
        Double myInterestScore,
        Double meAndFriendIntimacy,
        List<MutualFriendDetail> mutualFriends
) {

    public record MutualFriendDetail(
            Long mutualFriendId,
            Double mutualIntimacy
    ) {}

    @JsonProperty("normalizedMyInterestScore")
    public double getNormalizedMyInterest() {
        return FriendRecognition.normalize(myInterestScore);
    }

    @JsonProperty("normalizedIntimacy")
    public double getNormalizedIntimacy() {
        return meAndFriendIntimacy != null ? meAndFriendIntimacy : 0.0;
    }
}
