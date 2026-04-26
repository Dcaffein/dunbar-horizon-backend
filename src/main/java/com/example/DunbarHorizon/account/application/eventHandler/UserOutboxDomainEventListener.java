package com.example.DunbarHorizon.account.application.eventHandler;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class UserOutboxDomainEventListener {

    private final UserEventOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserActivated(UserActivatedEvent event) {
        String payload = buildPayload(event.userId(), event.nickname(), event.profileImageUrl());
        UserEventOutbox outbox = outboxRepository.save(
                UserEventOutbox.pending(event.userId(), UserOutboxEventType.ACTIVATE, payload)
        );
        registerAfterCommit(outbox, event.nickname(), event.profileImageUrl());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserDeactivated(UserDeactivatedEvent event) {
        UserEventOutbox outbox = outboxRepository.save(
                UserEventOutbox.pending(event.id(), UserOutboxEventType.DEACTIVATE, buildPayload(event.id(), null, null))
        );
        registerAfterCommit(outbox, null, null);
    }

    private void registerAfterCommit(UserEventOutbox outbox, String nickname, String profileImageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserSyncIntegrationEvent(
                        outbox.getId(),
                        outbox.getAggregateId(),
                        outbox.getEventType(),
                        nickname,
                        profileImageUrl
                ));
            }
        });
    }

    private String buildPayload(Long userId, String nickname, String profileImageUrl) {
        return String.format(
                "{\"userId\":%d,\"nickname\":\"%s\",\"profileImageUrl\":\"%s\"}",
                userId,
                nickname != null ? nickname : "",
                profileImageUrl != null ? profileImageUrl : ""
        );
    }
}
