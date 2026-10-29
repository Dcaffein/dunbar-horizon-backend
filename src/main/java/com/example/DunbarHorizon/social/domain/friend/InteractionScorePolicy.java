package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;

public class InteractionScorePolicy {

    private static final double FLAG_BASE = 10.0;
    private static final double FLAG_ENCORE_BASE = 20.0;
    private static final double PRESERVATION_BONUS_RATIO = 0.5;

    private InteractionScorePolicy() {}

    public static double scoreOf(InteractionType type) {
        return switch (type) {
            case VISIT                  -> 1.0;
            case BUZZ_SEND              -> 10.0;
            case BUZZ_REPLY             -> 2.0;
            case FLAG_ENDED             -> FLAG_BASE;
            case FLAG_ENDED_ENCORE      -> FLAG_ENCORE_BASE;
            case FLAG_PRESERVED         -> withPreservationBonus(FLAG_BASE);
            case FLAG_PRESERVED_ENCORE  -> withPreservationBonus(FLAG_ENCORE_BASE);
        };
    }

    private static double withPreservationBonus(double base) {
        return base * (1 + PRESERVATION_BONUS_RATIO);
    }
}
