package com.example.DunbarHorizon.global.event.interaction;

public record UserInteractionEvent(
        Long actorId,
        Long targetId,
        InteractionType type
) {}