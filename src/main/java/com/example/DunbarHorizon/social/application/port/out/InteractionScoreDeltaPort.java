package com.example.DunbarHorizon.social.application.port.out;

import java.util.Map;

public interface InteractionScoreDeltaPort {
    void accumulate(String friendshipId, Long userId, double delta);
    Map<String, Map<String, Double>> drainAll();
}
