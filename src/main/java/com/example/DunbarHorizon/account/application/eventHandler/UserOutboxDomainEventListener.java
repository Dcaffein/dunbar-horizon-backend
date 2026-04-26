package com.example.DunbarHorizon.account.application.eventHandler;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserProfileUpdatedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserOutboxDomainEventListener {

    private final UserEventOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserActivated(UserActivatedEvent event) {
        String payload = serialize(event.userId(), event.nickname(), event.profileImageUrl(), null);
        UserEventOutbox outbox = outboxRepository.save(
                UserEventOutbox.pending(event.userId(), UserOutboxEventType.ACTIVATE, payload)
        );
        registerAfterCommit(outbox, event.nickname(), event.profileImageUrl(), null);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserDeactivated(UserDeactivatedEvent event) {
        String payload = serialize(event.id(), null, null, null);
        UserEventOutbox outbox = outboxRepository.save(
                UserEventOutbox.pending(event.id(), UserOutboxEventType.DEACTIVATE, payload)
        );
        registerAfterCommit(outbox, null, null, null);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserProfileUpdated(UserProfileUpdatedEvent event) {
        String payload = serialize(event.userId(), event.nickname(), event.profileImageUrl(), event.occurredAt());
        UserEventOutbox outbox = outboxRepository.save(
                UserEventOutbox.pending(event.userId(), UserOutboxEventType.PROFILE_UPDATE, payload)
        );
        registerAfterCommit(outbox, event.nickname(), event.profileImageUrl(), event.occurredAt());
    }

    private void registerAfterCommit(UserEventOutbox outbox, String nickname, String profileImageUrl, LocalDateTime occurredAt) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserSyncIntegrationEvent(
                        outbox.getId(),
                        outbox.getAggregateId(),
                        outbox.getEventType(),
                        nickname,
                        profileImageUrl,
                        occurredAt
                ));
            }
        });
    }

    private String serialize(Long userId, String nickname, String profileImageUrl, LocalDateTime occurredAt) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("nickname", nickname);
        map.put("profileImageUrl", profileImageUrl);
        map.put("occurredAt", occurredAt != null ? occurredAt.toString() : null);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("[UserOutboxDomainEventListener] Failed to serialize payload for userId={}", userId);
            return "{}";
        }
    }
}
