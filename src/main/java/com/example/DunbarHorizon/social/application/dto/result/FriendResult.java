package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.dto.UserProfile;

public record FriendResult(
        Long id,
        String nickname
) {
    public static FriendResult from(UserProfile user) {
        return new FriendResult(
                user.id(),
                user.nickname()
        );
    }
}
