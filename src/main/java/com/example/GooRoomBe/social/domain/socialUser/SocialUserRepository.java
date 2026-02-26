package com.example.GooRoomBe.social.domain.socialUser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SocialUserRepository {
    Optional<SocialUser> findById(Long id);
    Set<UserReference> findAllUserReferencesById(Collection<Long> ids);
    SocialUser save(SocialUser socialUser);
    Set<SocialUser> saveAll(List<SocialUser> newUsers);
}