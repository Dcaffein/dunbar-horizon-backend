package com.example.DunbarHorizon.buzz.application.dto.command;

import java.util.List;

public record UpdateBuzzCommentCommand(
        Long requesterId,
        String buzzId,
        Long threadId,
        Long commentId,
        String text,
        List<String> imageKeys
) {}
