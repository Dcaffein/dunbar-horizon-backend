package com.example.DunbarHorizon.account.application.dto;

public record AuthTokenResult(
        String accessToken,
        String refreshToken
) {}
