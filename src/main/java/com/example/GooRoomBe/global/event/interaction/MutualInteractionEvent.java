package com.example.GooRoomBe.global.event.interaction;

public record MutualInteractionEvent(
        Long userIdA,
        Long userIdB,
        InteractionType type
) {}
