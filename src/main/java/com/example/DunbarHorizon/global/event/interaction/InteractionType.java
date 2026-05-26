package com.example.DunbarHorizon.global.event.interaction;

public enum InteractionType {
    VISIT(false),
    BUZZ_SEND(false),
    BUZZ_REPLY(false),
    FLAG_ENDED(true),
    FLAG_ENDED_ENCORE(true);

    private final boolean mutual;

    InteractionType(boolean mutual) { this.mutual = mutual; }

    public boolean isMutual() { return mutual; }
}
