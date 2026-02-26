package com.example.GooRoomBe.cast.domain.event;

import java.util.Set;

public record CastCreatedEvent(
        String castId,
        Long creatorId,
        Set<Long> recipientIds
) {}