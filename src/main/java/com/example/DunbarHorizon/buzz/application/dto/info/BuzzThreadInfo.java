package com.example.DunbarHorizon.buzz.application.dto.info;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;

public record BuzzThreadInfo(
        Long threadId,
        Long starterId,
        ThreadVisibility visibility,
        LocalDateTime createdAt
) {}
