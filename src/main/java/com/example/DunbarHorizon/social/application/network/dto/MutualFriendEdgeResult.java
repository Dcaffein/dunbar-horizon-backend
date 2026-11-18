package com.example.DunbarHorizon.social.application.network.dto;

public record MutualFriendEdgeResult(
        Long friendAId,
        Long friendBId,
        Double intimacy
) {}
