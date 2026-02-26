package com.example.GooRoomBe.flag.application.port.out;

public record FlagUserInfo(
        Long userId,
        String nickname,
        String profileImageUrl
) {}
