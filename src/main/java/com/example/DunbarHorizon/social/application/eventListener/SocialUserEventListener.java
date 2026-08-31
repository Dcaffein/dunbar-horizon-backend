package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.example.DunbarHorizon.social.application.service.SocialUserSyncCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUserEventListener {

    private final SocialUserSyncCommandService syncCommandService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSync(UserSyncIntegrationEvent event) {
        try {
            // 별도 빈의 트랜잭션 프록시가 커밋까지 마친 뒤에만 완료를 알린다.
            syncCommandService.sync(event);
            eventPublisher.publishEvent(new UserSyncCompletedEvent(event.outboxId()));
        } catch (Exception e) {
            log.error("[SocialUserEventListener] Sync failed — outboxId={}, userId={}, eventType={}",
                    event.outboxId(), event.userId(), event.eventType(), e);
        }
    }
}
