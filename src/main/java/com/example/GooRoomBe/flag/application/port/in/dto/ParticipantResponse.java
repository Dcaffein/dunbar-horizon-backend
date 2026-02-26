package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;

import java.time.LocalDateTime;

public record ParticipantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime joinedAt
) {
    public static ParticipantResponse of(FlagUserInfo userInfo, LocalDateTime joinedAt) {
        return new ParticipantResponse(
                userInfo.userId(),
                userInfo.nickname(),
                userInfo.profileImageUrl(),
                joinedAt
        );
    }
}