package com.example.DunbarHorizon.social.application.eventHandler;

import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SocialUserEventListener {

    private final SocialUserRepository socialUserRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserActivated(UserActivatedEvent event) {
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeactivated(UserDeactivatedEvent event) {
        socialUserRepository.findById(event.id())
                .ifPresent(socialUser -> {
                    socialUser.switchUserStatus(false);
                    socialUserRepository.save(socialUser);
                });
    }
}
