package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.Buzz;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailResult(
        String buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        long remainingMinutes,
        boolean isUnread,
        boolean isCreator,
        List<BuzzProfileResult> recipients,
        List<BuzzThreadSummaryResult> threads
) {
    public static BuzzDetailResult of(Buzz buzz, BuzzProfileResult author, List<String> imageUrls,
                                      boolean isUnread, boolean isCreator,
                                      List<BuzzProfileResult> recipients,
                                      List<BuzzThreadSummaryResult> threads, LocalDateTime now) {
        return new BuzzDetailResult(
                buzz.getId(),
                author,
                buzz.getText(),
                imageUrls,
                Math.max(0L, Duration.between(now, buzz.getExpiresAt()).toMinutes()),
                isUnread,
                isCreator,
                recipients,
                threads
        );
    }
}
