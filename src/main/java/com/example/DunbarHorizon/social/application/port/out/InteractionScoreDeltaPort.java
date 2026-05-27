package com.example.DunbarHorizon.social.application.port.out;

import java.util.Map;

public interface InteractionScoreDeltaPort {
    void accumulate(String friendshipId, Long userId, double delta);
    void accumulateMutual(String friendshipId, double delta);
    Map<String, FriendshipDelta> drainAll();
}
