package com.example.GooRoomBe.social.application.dto;

public record NetworkTwoHopSuggestionsResponse(
        Long suggestedFriendId,
        String suggestedFriendName,
        Long commonFriendId
) {}
