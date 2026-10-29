package com.example.DunbarHorizon.global.event.interaction;

public enum ScoreDirection {
    GRANT,
    REVOKE;

    public double signed(double score) {
        return this == REVOKE ? -score : score;
    }
}
