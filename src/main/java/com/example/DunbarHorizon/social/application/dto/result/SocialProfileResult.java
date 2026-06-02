package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record SocialProfileResult(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static SocialProfileResult from(UserReference ref) {
        return new SocialProfileResult(ref.getId(), ref.getNickname(), ref.getProfileImageUrl());
    }
}
