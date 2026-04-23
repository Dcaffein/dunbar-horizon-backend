package com.example.DunbarHorizon.social.application.dto.result;

public record NetworkFriendEdgeResult(
        Long friendAId,
        Long friendBId,
        Double intimacy,
        Double friendAInterest,
        Double friendBInterest
) {}
