package com.example.DunbarHorizon.buzz.application.dto.view;

public record BuzzAudienceView(
        Long userId,
        BuzzUserProfileView profile,
        boolean isRead
) {}
