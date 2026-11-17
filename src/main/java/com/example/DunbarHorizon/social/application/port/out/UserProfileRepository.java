package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.UserProfile;

import java.util.Collection;
import java.util.Map;

public interface UserProfileRepository {
    Map<Long, UserProfile> findActiveProfiles(Collection<Long> ids);
}
