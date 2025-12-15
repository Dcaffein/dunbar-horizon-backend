package com.example.GooRoomBe.global.event;

public record UserInteractionEvent(
        String actorId,
        String targetId,
        InteractionType type,
        double score
) {}