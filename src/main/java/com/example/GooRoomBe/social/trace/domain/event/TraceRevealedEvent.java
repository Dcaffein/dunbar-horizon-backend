package com.example.GooRoomBe.social.trace.domain.event;

public record TraceRevealedEvent(
        String visitorId,
        String visitorNickname,
        String targetId
) {}