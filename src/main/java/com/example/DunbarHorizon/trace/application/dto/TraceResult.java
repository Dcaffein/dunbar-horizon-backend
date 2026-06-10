package com.example.DunbarHorizon.trace.application.dto;

public record TraceResult(boolean revealed, Long revealedWithUserId) {

    public static TraceResult notRevealed() {
        return new TraceResult(false, null);
    }
}
