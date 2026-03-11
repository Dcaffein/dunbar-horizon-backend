package com.example.DunbarHorizon.account.application.dto;

import com.example.DunbarHorizon.account.domain.model.User;

public record UserProfileInfo(Long id, String nickname, String profileImage) {
    public static UserProfileInfo from(User user) {
        return new UserProfileInfo(user.getId(), user.getNickname(), user.getProfileImage());
    }
}
