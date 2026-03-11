package com.example.DunbarHorizon.global.event.user;

public record UserActivatedEvent(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
