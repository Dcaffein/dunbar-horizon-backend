package com.example.DunbarHorizon.buzz.application.dto.info;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzCommentInfo(
        Long commentId,
        Long authorId,
        BuzzUserProfileInfo authorProfile,
        String text,
        List<String> imageKeys,
        LocalDateTime createdAt
) {}
