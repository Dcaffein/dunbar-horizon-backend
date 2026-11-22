package com.example.DunbarHorizon.buzz.application.dto.command;

import java.util.List;

public record WriteBuzzCommentCommand(
        Long authorId,
        Long buzzId,
        Long threadId,
        String text,
        List<String> imageKeys
) {}
