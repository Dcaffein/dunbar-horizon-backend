package com.example.GooRoomBe.cast.adapter.in.web.dto;

import com.example.GooRoomBe.cast.domain.model.Cast;

import java.time.LocalDateTime;
import java.util.List;

public record CastDetailResponseDto(
        String castId,
        CastProfileResponse author,
        String text,
        List<String> imageUrls,
        List<CastReplyResponseDto> replies,
        long remainingMinutes,
        boolean isUnread
) {
    public static CastDetailResponseDto from(Cast cast, Long currentUserId) {
        return new CastDetailResponseDto(
                cast.getId(),
                new CastProfileResponse(cast.getCreatorId(), cast.getCreatorNickname(), cast.getCreatorProfileImageUrl()),
                cast.getText(),
                cast.getImageUrls(),
                cast.getReplies().stream().map(CastReplyResponseDto::from).toList(),
                Math.max(0L, java.time.Duration.between(LocalDateTime.now(), cast.getExpiresAt()).toMinutes()),
                cast.isUnreadBy(currentUserId)
        );
    }
}