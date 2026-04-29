package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BuzzReplyRequest(
        @NotBlank(message = "답장 내용은 비어있을 수 없습니다.")
        String text,
        boolean isPublic
){}