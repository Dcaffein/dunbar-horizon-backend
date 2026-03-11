package com.example.DunbarHorizon.social.application.dto.result;

public record MutualFriendEdgeResult(
        Long friendAId,
        Long friendBId,
        Double intimacy
) {}