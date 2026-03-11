package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserQueryUseCase {
    List<UserProfileInfo> getUserProfiles(Collection<Long> ids);
    Optional<UserProfileInfo> getActiveUserProfile(Long id);
}
