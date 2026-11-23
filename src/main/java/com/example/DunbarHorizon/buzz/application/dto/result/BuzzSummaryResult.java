package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzSummaryInfo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BuzzSummaryResult(
        Long buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime editedAt,
        long remainingMinutes,
        boolean isUnread,
        boolean isCreator
) {
    public static BuzzSummaryResult of(BuzzSummaryInfo buzz, BuzzProfileResult author, List<String> imageUrls,
                                       boolean isUnread, boolean isCreator, LocalDateTime now) {
        return new BuzzSummaryResult(
                buzz.buzzId(),
                author,
                buzz.text(),
                imageUrls,
                buzz.editedAt(),
                Math.max(0L, Duration.between(now, buzz.expiresAt()).toMinutes()),
                isUnread,
                isCreator
        );
    }
}
