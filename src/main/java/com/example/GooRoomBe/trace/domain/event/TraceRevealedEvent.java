package com.example.GooRoomBe.trace.domain.event;

public record TraceRevealedEvent(
        String visitorId,
        String visitorNickname,
        String targetId
) {}