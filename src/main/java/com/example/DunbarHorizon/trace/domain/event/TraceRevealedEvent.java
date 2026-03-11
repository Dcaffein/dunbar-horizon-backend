package com.example.DunbarHorizon.trace.domain.event;

public record TraceRevealedEvent(
        Long visitorId,
        Long targetId
) {}