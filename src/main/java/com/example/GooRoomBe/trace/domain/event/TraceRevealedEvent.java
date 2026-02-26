package com.example.GooRoomBe.trace.domain.event;

public record TraceRevealedEvent(
        Long visitorId,
        Long targetId
) {}