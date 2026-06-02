package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;

public interface SocialUserQueryUseCase {
    SocialProfileResult getSocialProfile(Long userId);
}
