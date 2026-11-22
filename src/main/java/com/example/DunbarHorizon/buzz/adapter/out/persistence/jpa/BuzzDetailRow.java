package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailRow(
        Long buzzId,
        Long creatorId,
        String text,
        List<String> imageKeys,
        LocalDateTime editedAt,
        LocalDateTime expiresAt,
        Long threadId,
        Long starterId,
        ThreadVisibility visibility,
        LocalDateTime threadCreatedAt
) {}
