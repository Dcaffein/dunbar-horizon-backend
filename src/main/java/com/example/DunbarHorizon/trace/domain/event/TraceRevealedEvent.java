package com.example.DunbarHorizon.trace.domain.event;

public record TraceRevealedEvent(
        Long minId,
        Long maxId
) {}
