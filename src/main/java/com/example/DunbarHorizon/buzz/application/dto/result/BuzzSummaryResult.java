package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.Buzz;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BuzzSummaryResult(
        String buzzId,
        BuzzProfileResult author,
        String text,
        List<String> imageUrls,
        int replyCount,
        long remainingMinutes,
        boolean isUnread
) {
    public static BuzzSummaryResult from(Buzz buzz, Long currentUserId) {
        return new BuzzSummaryResult(
                buzz.getId(),
                new BuzzProfileResult(buzz.getCreatorId(), buzz.getCreatorNickname(), buzz.getCreatorProfileImageUrl()),
                buzz.getText(),
                buzz.getImageUrls(),
                buzz.getReplies().size(),
                Duration.between(LocalDateTime.now(), buzz.getExpiresAt()).toMinutes(),
                buzz.isUnreadBy(currentUserId)
        );
    }
}
