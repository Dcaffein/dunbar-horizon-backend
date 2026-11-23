package com.example.DunbarHorizon.buzz.application.dto.view;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzDetailView(
        Long buzzId,
        Long creatorId,
        BuzzUserProfileView creatorProfile,
        String text,
        List<String> imageKeys,
        LocalDateTime editedAt,
        List<BuzzThreadView> threads
) {}
