package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.application.dto.MyProfileResult;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserQueryUseCase {
    List<UserProfileInfo> getUserProfiles(Collection<Long> ids);
    /** Neo4j sync 전용 — profileImage에 presigned URL이 아닌 raw S3 key를 담아 반환 */
    List<UserProfileInfo> getUserProfilesForSync(Collection<Long> ids);
    Optional<UserProfileInfo> getActiveUserProfile(Long id);
    Optional<UserProfileInfo> findActiveUserByEmail(String email);
    MyProfileResult getMyProfile(Long id);
}
