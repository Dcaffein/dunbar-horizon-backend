package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;

import java.util.Collection;
import java.util.Map;

public interface SocialUserProfileReader {
    Map<Long, SocialUserProfile> findActiveProfiles(Collection<Long> ids);
}
