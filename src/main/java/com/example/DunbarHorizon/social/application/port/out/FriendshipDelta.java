package com.example.DunbarHorizon.social.application.port.out;

import java.util.Map;

public record FriendshipDelta(
        Map<Long, Double> unilateral,
        double mutual
) {
    public boolean hasMutual() { return mutual > 0; }
    public boolean hasUnilateral() { return !unilateral.isEmpty(); }
}
