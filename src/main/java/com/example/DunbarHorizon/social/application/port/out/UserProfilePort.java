package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfilePort {
    Optional<UserProfileInfo> getActiveUserProfile(Long id);
    List<UserProfileInfo> getUserProfiles(Collection<Long> ids);
}
