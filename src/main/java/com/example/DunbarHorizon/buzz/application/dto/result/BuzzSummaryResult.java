package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzSummaryView;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzSummaryResult(
        Long buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        LocalDateTime editedAt,
        boolean isUnread,
        boolean isCreator
) {
    public static BuzzSummaryResult of(BuzzSummaryView buzz, BuzzProfileResult author, List<String> imageUrls,
                                       boolean isUnread, boolean isCreator) {
        return new BuzzSummaryResult(
                buzz.buzzId(),
                author,
                buzz.text(),
                imageUrls,
                buzz.editedAt(),
                isUnread,
                isCreator
        );
    }
}
