package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.in.SocialUserQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccountSocialUserQueryService implements SocialUserQueryUseCase {

    private final UserProfilePort userProfilePort;

    @Override
    public SocialProfileResult getSocialProfile(Long userId) {
        return userProfilePort.getActiveUserProfile(userId)
                .map(profile -> new SocialProfileResult(profile.id(), profile.nickname(), profile.profileImage()))
                .orElseThrow(() -> new UserReferenceNotFoundException(userId));
    }
}
