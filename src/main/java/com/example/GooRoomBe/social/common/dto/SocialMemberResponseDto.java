package com.example.GooRoomBe.social.common.dto;

import com.example.GooRoomBe.global.userReference.SocialUser;

public record SocialMemberResponseDto(
        String id,
        String nickname
) {
    public static SocialMemberResponseDto from(SocialUser user) {
        return new SocialMemberResponseDto(
                user.getId(),
                user.getNickname()
        );
    }
}