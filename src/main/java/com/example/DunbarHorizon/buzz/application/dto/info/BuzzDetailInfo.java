package com.example.DunbarHorizon.buzz.application.dto.info;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailInfo(
        Long buzzId,
        Long creatorId,
        BuzzUserProfileInfo creatorProfile,
        String text,
        List<String> imageKeys,
        LocalDateTime editedAt,
        List<BuzzThreadInfo> threads
) {}
