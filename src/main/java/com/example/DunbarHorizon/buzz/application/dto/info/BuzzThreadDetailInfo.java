package com.example.DunbarHorizon.buzz.application.dto.info;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzThreadDetailInfo(
        Long buzzId,
        Long creatorId,
        BuzzUserProfileInfo creatorProfile,
        ThreadContent thread
) {
    public record ThreadContent(
            Long threadId,
            Long starterId,
            BuzzUserProfileInfo starterProfile,
            ThreadVisibility visibility,
            String text,
            List<String> imageKeys,
            LocalDateTime createdAt
    ) {}
}
