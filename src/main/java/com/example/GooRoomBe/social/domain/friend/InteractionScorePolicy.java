package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.global.event.interaction.InteractionType;

public class InteractionScorePolicy {

    private InteractionScorePolicy() {}

    public static double scoreOf(InteractionType type) {
        return switch (type) {
            case VISIT              -> 1.0;
            case CAST_SEND          -> 1.5;
            case CAST_REPLY         -> 2.0;
            case FLAG_ENDED         -> 5.0;
            case FLAG_ENDED_ENCORE  -> 8.0;
        };
    }
}
