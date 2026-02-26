package com.example.GooRoomBe.global.event.interaction;

public record UserInteractionEvent(
        Long actorId,
        Long targetId,
        InteractionType type
) {}