package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.out.FriendshipQueryRepository;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountSocialUserQueryService {

    private final FriendshipQueryRepository friendshipQueryRepository;
    private final ImageUrlResolverPort images;

    public SocialProfileResult getSocialProfile(Long userId) {
        return friendshipQueryRepository.findActiveProfile(userId)
                .map(profile -> SocialProfileResult.from(profile, images))
                .orElseThrow(() -> new UserReferenceNotFoundException(userId));
    }
}
