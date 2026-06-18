package com.example.DunbarHorizon.account.application.dto;

public record MyProfileResult(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {}
