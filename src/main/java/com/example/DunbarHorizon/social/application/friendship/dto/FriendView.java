package com.example.DunbarHorizon.social.application.friendship.dto;

public record FriendView(
        Long friendId,
        String nickname,
        String imageKey,
        String friendAlias,
        double intimacy,
        double myNormalizedInterestScore,
        boolean muted,
        boolean routable
) {}
