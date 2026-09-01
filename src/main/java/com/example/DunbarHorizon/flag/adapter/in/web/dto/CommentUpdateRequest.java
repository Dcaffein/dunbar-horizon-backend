package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank(message = FlagComment.CONTENT_LENGTH_MESSAGE)
        @Size(max = FlagComment.CONTENT_MAX_LENGTH, message = FlagComment.CONTENT_LENGTH_MESSAGE)
        String content,
        boolean isPrivate
) {}
