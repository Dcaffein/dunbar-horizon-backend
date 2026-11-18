package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.dto.UserProfile;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountSocialUserQueryService {

    private final UserProfileRepository userProfileRepository;
    private final ImageUrlResolverPort images;

    public SocialProfileResult getSocialProfile(Long userId) {
        UserProfile profile = userProfileRepository.findActiveProfiles(List.of(userId)).get(userId);
        if (profile == null) {
            throw new UserReferenceNotFoundException(userId);
        }
        return SocialProfileResult.from(profile, images);
    }
}
