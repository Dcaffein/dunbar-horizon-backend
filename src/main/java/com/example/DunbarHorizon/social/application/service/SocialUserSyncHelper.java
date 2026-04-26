package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUserSyncHelper {

    private final SocialUserRepository socialUserRepository;
    private final UserProfilePort userProfilePort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<UserReference> syncAndSave(Set<Long> missingIds) {
        List<UserProfileInfo> profiles = userProfilePort.getUserProfiles(missingIds);
        List<SocialUser> newUsers = profiles.stream()
                .map(dto -> new SocialUser(dto.id(), dto.nickname(), dto.profileImage()))
                .toList();

        try {
            return socialUserRepository.saveAll(newUsers).stream()
                    .map(user -> (UserReference) user)
                    .collect(Collectors.toSet());
        } catch (DataIntegrityViolationException e) {
            // 스케줄러 재시도와 Lazy Sync가 동시에 발생한 경우 — 이미 생성된 노드이므로 정상 처리
            log.debug("[SocialUserSyncHelper] Concurrent create detected, node already exists. ids={}", missingIds);
            return socialUserRepository.findAllUserReferencesById(missingIds).stream()
                    .map(user -> (UserReference) user)
                    .collect(Collectors.toSet());
        }
    }
}
