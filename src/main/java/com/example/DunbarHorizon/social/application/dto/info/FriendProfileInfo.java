package com.example.DunbarHorizon.social.application.dto.info;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record FriendProfileInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static FriendProfileInfo from(UserReference user) {
        return new FriendProfileInfo(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
