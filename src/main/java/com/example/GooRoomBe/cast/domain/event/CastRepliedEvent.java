package com.example.GooRoomBe.cast.domain.event;

public record CastRepliedEvent(
        String castId,
        Long creatorId,
        Long replierId
) {}