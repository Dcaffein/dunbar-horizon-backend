package com.example.DunbarHorizon.buzz.domain.event;

public record BuzzCommentedEvent(
        String buzzId,
        Long creatorId,
        Long commenterId
) {}
