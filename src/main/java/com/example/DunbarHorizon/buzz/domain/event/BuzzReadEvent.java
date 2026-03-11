package com.example.DunbarHorizon.buzz.domain.event;

public record BuzzReadEvent(
        String buzzId, Long userId
) {}
