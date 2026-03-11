package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record FriendResult(
        Long id,
        String nickname
) {
    public static FriendResult from(UserReference user) {
        return new FriendResult(
                user.getId(),
                user.getNickname()
        );
    }
}
