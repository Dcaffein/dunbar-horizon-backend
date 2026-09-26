package com.example.DunbarHorizon.social.domain.friend;

public record MutualInterestScoreUpdate(
        String friendshipId,
        Long userAId,
        Long userBId,
        double delta
) {}
