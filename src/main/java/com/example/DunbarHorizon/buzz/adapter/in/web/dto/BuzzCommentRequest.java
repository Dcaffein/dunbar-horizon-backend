package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BuzzCommentRequest(
        @NotBlank(message = "댓글 내용은 비어있을 수 없습니다.")
        String text,
        boolean isPublic,
        List<String> imageKeys
) {}
