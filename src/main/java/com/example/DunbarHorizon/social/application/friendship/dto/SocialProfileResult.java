package com.example.DunbarHorizon.social.application.friendship.dto;

import com.example.DunbarHorizon.social.application.friendship.port.ImageUrlResolverPort;

public record SocialProfileResult(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static SocialProfileResult from(UserProfile ref, ImageUrlResolverPort resolver) {
        return new SocialProfileResult(ref.id(), ref.nickname(),
                resolver.resolveUrl(ref.imageKey()));
    }
}
