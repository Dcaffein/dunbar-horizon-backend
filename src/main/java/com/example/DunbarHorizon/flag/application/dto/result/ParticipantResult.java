package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;

public record ParticipantResult(
        Long id,
        String nickname,
        String profileImageUrl,
        boolean canInvite
) {
    public static ParticipantResult of(FlagUserInfo userInfo, boolean canInvite) {
        return new ParticipantResult(
                userInfo.userId(),
                userInfo.nickname(),
                userInfo.profileImageUrl(),
                canInvite
        );
    }
}
