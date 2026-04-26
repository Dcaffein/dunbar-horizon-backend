package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOutboxRetryService {

    private static final int MAX_RETRY = 5;

    private final UserEventOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void retryPending() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<UserEventOutbox> pendingList = outboxRepository.findPendingOlderThan(threshold);

        for (UserEventOutbox outbox : pendingList) {
            if (outbox.getRetryCount() >= MAX_RETRY) {
                outbox.markFailed();
                outboxRepository.save(outbox);
                log.error("[UserOutbox] Dead letter — outboxId={}, aggregateId={}, eventType={}",
                        outbox.getId(), outbox.getAggregateId(), outbox.getEventType());
                continue;
            }
            outbox.incrementRetry();
            outboxRepository.save(outbox);
            eventPublisher.publishEvent(buildIntegrationEvent(outbox));
        }
    }

    @EventListener
    @Transactional
    public void onSyncCompleted(UserSyncCompletedEvent event) {
        outboxRepository.findById(event.outboxId()).ifPresent(outbox -> {
            outbox.markCompleted();
            outboxRepository.save(outbox);
        });
    }

    private UserSyncIntegrationEvent buildIntegrationEvent(UserEventOutbox outbox) {
        Map<String, Object> payload = parsePayload(outbox.getPayload());
        String nickname = (String) payload.get("nickname");
        String profileImageUrl = (String) payload.get("profileImageUrl");
        LocalDateTime occurredAt = Optional.ofNullable((String) payload.get("occurredAt"))
                .map(LocalDateTime::parse)
                .orElse(null);

        return new UserSyncIntegrationEvent(
                outbox.getId(),
                outbox.getAggregateId(),
                outbox.getEventType(),
                nickname,
                profileImageUrl,
                occurredAt
        );
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[UserOutboxRetryService] Failed to parse payload: {}", payload);
            return Map.of();
        }
    }
}
