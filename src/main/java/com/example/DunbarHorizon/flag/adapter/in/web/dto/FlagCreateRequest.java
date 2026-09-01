package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record FlagCreateRequest(
        Long parentFlagId,
        @NotBlank(message = Flag.TITLE_LENGTH_MESSAGE)
        @Size(max = Flag.TITLE_MAX_LENGTH, message = Flag.TITLE_LENGTH_MESSAGE)
        String title,
        @NotBlank(message = Flag.DESCRIPTION_LENGTH_MESSAGE)
        @Size(max = Flag.DESCRIPTION_MAX_LENGTH, message = Flag.DESCRIPTION_LENGTH_MESSAGE)
        String description,
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다.") Integer capacity,
        LocalDateTime deadline,
        @NotNull(message = "시작 시간은 필수입니다.") LocalDateTime startDateTime,
        @NotNull(message = "종료 시간은 필수입니다.") LocalDateTime endDateTime
) { }