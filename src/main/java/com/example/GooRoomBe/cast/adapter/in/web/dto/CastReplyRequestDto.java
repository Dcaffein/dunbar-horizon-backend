package com.example.GooRoomBe.cast.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CastReplyRequestDto(
        @NotBlank(message = "답장 내용은 비어있을 수 없습니다.")
        String text,
        List<String> imageUrls,
        boolean isPublic
){}