package com.example.GooRoomBe.cast.adapter.in.web.dto;

import com.example.GooRoomBe.cast.domain.model.Cast;

import java.time.LocalDateTime;
import java.util.List;

public record CastSummaryResponseDto(
        String castId,
        CastProfileResponse author,
        String text,
        List<String> imageUrls,
        int replyCount,
        long remainingMinutes,
        boolean isUnread
) {
    public static CastSummaryResponseDto from(Cast cast, Long currentUserId) {
        return new CastSummaryResponseDto(
                cast.getId(),
                new CastProfileResponse(cast.getCreatorId(), cast.getCreatorNickname(), cast.getCreatorProfileImageUrl()),
                cast.getText(),
                cast.getImageUrls(),
                cast.getReplies().size(),
                java.time.Duration.between(LocalDateTime.now(), cast.getExpiresAt()).toMinutes(),
                cast.isUnreadBy(currentUserId)
        );
    }
}