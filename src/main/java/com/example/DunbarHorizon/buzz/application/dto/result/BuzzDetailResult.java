package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailResult(
        Long buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime editedAt,
        long remainingMinutes,
        boolean isUnread,
        boolean isCreator,
        List<BuzzProfileResult> recipients,
        List<BuzzThreadSummaryResult> threads
) {
    public static BuzzDetailResult of(BuzzDetailInfo buzz, BuzzProfileResult author, List<String> imageUrls,
                                      boolean isUnread, boolean isCreator,
                                      List<BuzzProfileResult> recipients,
                                      List<BuzzThreadSummaryResult> threads, LocalDateTime now) {
        return new BuzzDetailResult(
                buzz.buzzId(),
                author,
                buzz.text(),
                imageUrls,
                buzz.editedAt(),
                Math.max(0L, Duration.between(now, buzz.expiresAt()).toMinutes()),
                isUnread,
                isCreator,
                recipients,
                threads
        );
    }
}
