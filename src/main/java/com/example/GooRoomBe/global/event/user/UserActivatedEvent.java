package com.example.GooRoomBe.global.event.user;

public record UserActivatedEvent(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
