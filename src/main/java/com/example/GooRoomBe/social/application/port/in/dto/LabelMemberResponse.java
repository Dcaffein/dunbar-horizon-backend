package com.example.GooRoomBe.social.application.port.in.dto;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;

public record LabelMemberResponse(
        Long id,
        String nickname
) {
    public static LabelMemberResponse from(UserReference user) {
        return new LabelMemberResponse(
                user.getId(),
                user.getNickname()
        );
    }
}