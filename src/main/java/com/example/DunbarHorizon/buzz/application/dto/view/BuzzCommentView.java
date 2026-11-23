package com.example.DunbarHorizon.buzz.application.dto.view;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzCommentView(
        Long commentId,
        Long authorId,
        BuzzUserProfileView authorProfile,
        String text,
        List<String> imageKeys,
        LocalDateTime createdAt
) {}
