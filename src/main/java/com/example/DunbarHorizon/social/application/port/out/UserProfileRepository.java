package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;

import java.util.Collection;
import java.util.Map;

public interface UserProfileRepository {
    Map<Long, SocialUserProfile> findActiveProfiles(Collection<Long> ids);
}
