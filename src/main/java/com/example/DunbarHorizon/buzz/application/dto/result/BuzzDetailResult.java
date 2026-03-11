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
        List<BuzzReplyResult> replies,
        long remainingMinutes,
        boolean isUnread
) {
    public static BuzzDetailResult from(Buzz buzz, Long currentUserId) {
        return new BuzzDetailResult(
                buzz.getId(),
                new BuzzProfileResult(buzz.getCreatorId(), buzz.getCreatorNickname(), buzz.getCreatorProfileImageUrl()),
                buzz.getText(),
                buzz.getImageUrls(),
                buzz.getReplies().stream().map(BuzzReplyResult::from).toList(),
                Math.max(0L, Duration.between(LocalDateTime.now(), buzz.getExpiresAt()).toMinutes()),
                buzz.isUnreadBy(currentUserId)
        );
    }
}
