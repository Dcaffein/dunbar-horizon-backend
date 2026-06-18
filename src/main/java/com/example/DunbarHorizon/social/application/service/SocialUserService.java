package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.in.SocialUserQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Neo4jTransactional
public class SocialUserService implements SocialUserQueryUseCase {
    private final SocialUserRepository socialUserRepository;
    private final SocialUserSyncHelper syncHelper;
    private final ImageUrlResolverPort imageUrlResolverPort;

    @Override
    public SocialProfileResult getSocialProfile(Long userId) {
        return SocialProfileResult.from(getUserReference(userId), imageUrlResolverPort);
    }

    public UserReference getUserReference(Long userId) {
        return getUserReferences(Set.of(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new UserReferenceNotFoundException(userId));
    }

    public Set<UserReference> getUserReferences(Collection<Long> userIds) {
        Set<Long> requestedIds = StreamSupport.stream(userIds.spliterator(), false)
                .collect(Collectors.toSet());

        Set<UserReference> existingUsers = socialUserRepository.findAllUserReferencesById(requestedIds);

        Set<Long> existingIds = existingUsers.stream()
                .map(UserReference::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = requestedIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toSet());

        if (missingIds.isEmpty()) {
            return existingUsers;
        }

        Set<UserReference> syncedUsers = syncHelper.syncAndSave(missingIds);

        return Stream.concat(existingUsers.stream(), syncedUsers.stream())
                .collect(Collectors.toSet());
    }
}
