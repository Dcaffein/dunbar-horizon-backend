package com.example.GooRoomBe.account.application.port.in.dto;

public record AuthTokenResult(
        String accessToken,
        String refreshToken
) {}