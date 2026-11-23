package com.example.DunbarHorizon.buzz.application.dto.info;

public record BuzzAudienceInfo(
        Long userId,
        BuzzUserProfileInfo profile,
        boolean isRead
) {}
