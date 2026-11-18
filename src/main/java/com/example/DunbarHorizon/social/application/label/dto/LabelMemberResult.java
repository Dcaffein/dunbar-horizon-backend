package com.example.DunbarHorizon.social.application.label.dto;

import com.example.DunbarHorizon.social.application.friendship.dto.UserProfile;

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
