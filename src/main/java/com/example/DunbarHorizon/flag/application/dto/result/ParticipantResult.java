package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;

public record ParticipantResult(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static ParticipantResult of(FlagUserInfo userInfo) {
        return new ParticipantResult(
                userInfo.userId(),
                userInfo.nickname(),
                userInfo.profileImageUrl()
        );
    }
}
