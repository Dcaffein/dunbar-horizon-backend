package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.application.dto.UserProfile;

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
