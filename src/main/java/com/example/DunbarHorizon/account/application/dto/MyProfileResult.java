package com.example.DunbarHorizon.account.application.dto;

import com.example.DunbarHorizon.account.domain.User;

public record MyProfileResult(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {
    public static MyProfileResult from(User user) {
        return new MyProfileResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
        );
    }
}
