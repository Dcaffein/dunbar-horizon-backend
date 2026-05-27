package com.example.DunbarHorizon.global.event.interaction;

public record UserInteractionEvent(
        Long userA,
        Long userB,
        InteractionType type
) {}