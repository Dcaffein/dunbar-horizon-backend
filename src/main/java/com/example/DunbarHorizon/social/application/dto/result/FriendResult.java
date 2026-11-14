package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;

public record FriendResult(
        Long id,
        String nickname
) {
    public static FriendResult from(SocialUserProfile user) {
        return new FriendResult(
                user.id(),
                user.nickname()
        );
    }
}
