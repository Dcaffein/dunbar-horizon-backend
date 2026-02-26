package com.example.GooRoomBe.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record FlagDetailsUpdateRequest(
        @NotBlank String title,
        @NotBlank String description
) {}
