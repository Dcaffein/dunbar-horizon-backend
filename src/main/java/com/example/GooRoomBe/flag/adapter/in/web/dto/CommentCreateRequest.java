package com.example.GooRoomBe.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank String content,
        boolean isPrivate) {
}
