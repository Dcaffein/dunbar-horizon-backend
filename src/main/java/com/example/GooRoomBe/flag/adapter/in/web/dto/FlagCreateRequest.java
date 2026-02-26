package com.example.GooRoomBe.flag.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FlagCreateRequest(
        Long parentFlagId,
        @NotBlank String title,
        @NotBlank String description,
        @Min(1) Integer capacity,
        LocalDateTime deadline,
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime
) { }