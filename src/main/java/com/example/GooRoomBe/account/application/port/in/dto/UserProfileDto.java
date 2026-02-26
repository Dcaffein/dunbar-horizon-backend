package com.example.GooRoomBe.account.application.port.in.dto;

import com.example.GooRoomBe.account.domain.model.User;

public record UserProfileDto(Long id, String nickname, String profileImage) {
    public static UserProfileDto from(User user) {
        return new UserProfileDto(user.getId(), user.getNickname(), user.getProfileImage());
    }
}