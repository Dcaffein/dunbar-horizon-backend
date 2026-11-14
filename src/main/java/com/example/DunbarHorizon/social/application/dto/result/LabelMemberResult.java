package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;

public record LabelMemberResult(
        Long id,
        String nickname
) {
    public static LabelMemberResult from(SocialUserProfile user) {
        return new LabelMemberResult(
                user.id(),
                user.nickname()
        );
    }
}
