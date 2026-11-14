package com.example.DunbarHorizon.social.application.dto;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public record SocialUserProfile(Long id, String nickname, String imageKey) implements UserReference {
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public String getProfileImageUrl() {
        return imageKey;
    }
}
