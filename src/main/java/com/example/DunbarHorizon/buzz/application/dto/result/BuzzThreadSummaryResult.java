package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;

public record BuzzThreadSummaryResult(
        Long threadId,
        BuzzProfileResult starter,
        ThreadVisibility visibility,
        LocalDateTime createdAt
) {
    public static BuzzThreadSummaryResult of(BuzzThread thread, BuzzProfileResult starter) {
        return new BuzzThreadSummaryResult(thread.getId(), starter, thread.getVisibility(), thread.getCreatedAt());
    }
}
