package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlagDetailsUpdateRequest(
        @NotBlank(message = Flag.TITLE_LENGTH_MESSAGE)
        @Size(max = Flag.TITLE_MAX_LENGTH, message = Flag.TITLE_LENGTH_MESSAGE)
        String title,
        @NotBlank(message = Flag.DESCRIPTION_LENGTH_MESSAGE)
        @Size(max = Flag.DESCRIPTION_MAX_LENGTH, message = Flag.DESCRIPTION_LENGTH_MESSAGE)
        String description
) {}
