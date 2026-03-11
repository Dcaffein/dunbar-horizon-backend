package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record LabelMemberResult(
        Long id,
        String nickname
) {
    public static LabelMemberResult from(UserReference user) {
        return new LabelMemberResult(
                user.getId(),
                user.getNickname()
        );
    }
}
