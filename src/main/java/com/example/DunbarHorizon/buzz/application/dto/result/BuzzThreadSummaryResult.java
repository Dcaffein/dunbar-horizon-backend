package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;

public record BuzzThreadSummaryResult(
        Long threadId,
        BuzzProfileResult starter,
        ThreadVisibility visibility,
        LocalDateTime createdAt
) {
    public static BuzzThreadSummaryResult of(BuzzThreadInfo thread, BuzzProfileResult starter) {
        return new BuzzThreadSummaryResult(thread.threadId(), starter, thread.visibility(), thread.createdAt());
    }
}
