package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;

import java.time.LocalDateTime;

public record ParticipantResult(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime joinedAt
) {
    public static ParticipantResult of(FlagUserInfo userInfo, LocalDateTime joinedAt) {
        return new ParticipantResult(
                userInfo.userId(),
                userInfo.nickname(),
                userInfo.profileImageUrl(),
                joinedAt
        );
    }
}
