package com.example.DunbarHorizon.global.event.interaction;

public record MutualInteractionEvent(
        Long userIdA,
        Long userIdB,
        InteractionType type
) {}
