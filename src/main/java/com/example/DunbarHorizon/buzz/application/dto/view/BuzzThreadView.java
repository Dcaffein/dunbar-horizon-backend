package com.example.DunbarHorizon.buzz.application.dto.view;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;

public record BuzzThreadView(
        Long threadId,
        Long starterId,
        BuzzUserProfileView starterProfile,
        ThreadVisibility visibility,
        LocalDateTime createdAt
) {}
