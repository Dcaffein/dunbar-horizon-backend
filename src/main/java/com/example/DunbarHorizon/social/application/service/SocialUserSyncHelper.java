package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SocialUserSyncHelper {
    private final SocialUserRepository socialUserRepository;
    private final UserQueryUseCase userQueryUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<UserReference> syncAndSave(Set<Long> missingIds) {
        List<UserProfileInfo> profiles = userQueryUseCase.getUserProfiles(missingIds);
        List<SocialUser> newUsers = profiles.stream()
                .map(dto -> new SocialUser(
                        dto.id(),
                        dto.nickname(),
                        dto.profileImage()))
                .toList();

        return socialUserRepository.saveAll(newUsers).stream()
                .map(user -> (UserReference) user)
                .collect(Collectors.toSet());
    }
}
