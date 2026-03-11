package com.example.DunbarHorizon.social.application.dto.result;

public record NetworkTwoHopSuggestionsResult(
        Long suggestedFriendId,
        String suggestedFriendName,
        Long commonFriendId
) {}
