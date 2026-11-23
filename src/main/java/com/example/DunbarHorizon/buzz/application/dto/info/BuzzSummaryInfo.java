package com.example.DunbarHorizon.buzz.application.dto.info;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzSummaryInfo(
        Long buzzId,
        Long creatorId,
        BuzzUserProfileInfo creatorProfile,
        String text,
        List<String> imageKeys,
        LocalDateTime editedAt,
        boolean isRead
) {}
