package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialUserService {
    private final SocialUserRepository socialUserRepository;
    private final SocialUserSyncHelper syncHelper;

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
