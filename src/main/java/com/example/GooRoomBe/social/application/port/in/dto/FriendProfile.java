package com.example.GooRoomBe.social.application.port.in.dto;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;

public record FriendProfile(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static FriendProfile from(UserReference user) {
        return new FriendProfile(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}