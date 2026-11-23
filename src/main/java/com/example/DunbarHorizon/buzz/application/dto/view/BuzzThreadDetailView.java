package com.example.DunbarHorizon.buzz.application.dto.view;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzThreadDetailView(
        Long buzzId,
        Long creatorId,
        BuzzUserProfileView creatorProfile,
        ThreadContent thread
) {
    public record ThreadContent(
            Long threadId,
            Long starterId,
            BuzzUserProfileView starterProfile,
            ThreadVisibility visibility,
            String text,
            List<String> imageKeys,
            LocalDateTime createdAt
    ) {}
}
