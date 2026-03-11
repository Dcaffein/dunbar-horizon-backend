package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;

public class InteractionScorePolicy {

    private InteractionScorePolicy() {}

    public static double scoreOf(InteractionType type) {
        return switch (type) {
            case VISIT              -> 1.0;
            case BUZZ_SEND          -> 10.0;
            case BUZZ_REPLY         -> 2.0;
            case FLAG_ENDED         -> 10.0;
            case FLAG_ENDED_ENCORE  -> 20.0;
        };
    }
}
