package com.example.DunbarHorizon.social.application.eventHandler;

import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUserEventListener {

    private final SocialUserRepository socialUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void onUserSync(UserSyncIntegrationEvent event) {
        try {
            if (event.eventType() == UserOutboxEventType.ACTIVATE) {
                handleActivate(event);
            } else if (event.eventType() == UserOutboxEventType.DEACTIVATE) {
                handleDeactivate(event);
            }
            eventPublisher.publishEvent(new UserSyncCompletedEvent(event.outboxId()));
        } catch (Exception e) {
            log.error("[SocialUserEventListener] Sync failed — outboxId={}, userId={}, eventType={}",
                    event.outboxId(), event.userId(), event.eventType(), e);
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
}
