package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.UserProfile;

public record LabelMemberResult(
        Long id,
        String nickname
) {
    public static LabelMemberResult from(UserProfile user) {
        return new LabelMemberResult(
                user.id(),
                user.nickname()
        );
    }
}
