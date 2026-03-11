package com.example.DunbarHorizon.buzz.domain.event;

import java.util.Set;

public record BuzzCreatedEvent(
        String buzzId,
        Long creatorId,
        Set<Long> recipientIds
) {}