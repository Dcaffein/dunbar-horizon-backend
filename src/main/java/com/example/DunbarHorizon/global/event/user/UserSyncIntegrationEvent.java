package com.example.DunbarHorizon.global.event.user;

import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;

public record UserSyncIntegrationEvent(
        String outboxId,
        Long userId,
        UserOutboxEventType eventType,
        String nickname,
        String profileImageUrl
) {
}
