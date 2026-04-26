package com.example.DunbarHorizon.global.event.user;

import java.time.LocalDateTime;

public record UserProfileUpdatedEvent(
        Long userId,
        String nickname,
        String profileImageUrl,
        LocalDateTime occurredAt
) {
}
