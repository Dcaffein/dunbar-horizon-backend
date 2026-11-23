package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailResult(
        Long buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime editedAt,
        boolean isUnread,
        boolean isCreator,
        List<BuzzProfileResult> recipients,
        List<BuzzThreadSummaryResult> threads
) {
    public static BuzzDetailResult of(BuzzDetailInfo buzz, BuzzProfileResult author, List<String> imageUrls,
                                      boolean isUnread, boolean isCreator,
                                      List<BuzzProfileResult> recipients,
                                      List<BuzzThreadSummaryResult> threads) {
        return new BuzzDetailResult(
                buzz.buzzId(),
                author,
                buzz.text(),
                imageUrls,
                buzz.editedAt(),
                isUnread,
                isCreator,
                recipients,
                threads
        );
    }
}
