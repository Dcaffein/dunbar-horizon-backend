package com.example.DunbarHorizon.buzz.application.dto.command;

import java.util.List;

public record UpdateBuzzCommentCommand(
        Long requesterId,
        Long buzzId,
        Long threadId,
        Long commentId,
        String text,
        List<String> imageKeys
) {}
