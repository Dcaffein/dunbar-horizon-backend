package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BuzzCommentRequest(
        @NotBlank(message = "댓글 내용은 비어있을 수 없습니다.")
        @Size(max = BuzzComment.TEXT_MAX_LENGTH, message = BuzzComment.TEXT_LENGTH_MESSAGE)
        String text,
        boolean isPublic,
        List<String> imageKeys
) {}
