package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class SocialUserSyncCommandService {

    private final SocialUserRepository socialUserRepository;

    @Neo4jTransactional(propagation = Propagation.REQUIRES_NEW)
    public void sync(UserSyncIntegrationEvent event) {
        switch (event.eventType()) {
            case ACTIVATE -> handleActivate(event);
            case DEACTIVATE -> handleDeactivate(event);
            case PROFILE_UPDATE -> handleProfileUpdate(event);
        }
    }

    private void handleActivate(UserSyncIntegrationEvent event) {
        socialUserRepository.findById(event.userId())
                .ifPresentOrElse(
                        socialUser -> {
                            socialUser.switchUserStatus(true);
                            socialUserRepository.save(socialUser);
                        },
                        () -> socialUserRepository.save(
                                new SocialUser(event.userId(), event.nickname(), event.profileImageUrl())
                        )
                );
    }

    private void handleDeactivate(UserSyncIntegrationEvent event) {
        socialUserRepository.findById(event.userId())
                .ifPresent(socialUser -> {
                    socialUser.switchUserStatus(false);
                    socialUserRepository.save(socialUser);
                });
    }

    private void handleProfileUpdate(UserSyncIntegrationEvent event) {
        socialUserRepository.findById(event.userId())
                .ifPresent(socialUser -> {
                    socialUser.updateProfile(event.nickname(), event.profileImageUrl(), event.occurredAt());
                    socialUserRepository.save(socialUser);
                });
    }
}
