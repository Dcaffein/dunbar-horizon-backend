package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserProfilePort {
    Optional<UserProfileInfo> getActiveUserProfile(Long id);
    List<UserProfileInfo> getUserProfiles(Collection<Long> ids);
    /** Neo4j sync 전용 — profileImage에 raw S3 key를 담아 반환 */
    List<UserProfileInfo> getUserProfilesForSync(Collection<Long> ids);
}
