package com.example.DunbarHorizon.buzz.domain.event;

public record BuzzRepliedEvent(
        String buzzId,
        Long creatorId,
        Long replierId
) {}