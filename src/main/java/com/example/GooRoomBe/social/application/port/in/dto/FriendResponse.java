package com.example.GooRoomBe.social.application.port.in.dto;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;

public record FriendResponse(
        Long id,
        String nickname
) {
    public static FriendResponse from(UserReference user) {
        return new FriendResponse(
                user.getId(),
                user.getNickname()
        );
    }
}