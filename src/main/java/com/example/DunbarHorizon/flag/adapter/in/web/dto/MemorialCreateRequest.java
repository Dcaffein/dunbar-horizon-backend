package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemorialCreateRequest(
        @NotBlank(message = FlagMemorial.CONTENT_LENGTH_MESSAGE)
        @Size(max = FlagMemorial.CONTENT_MAX_LENGTH, message = FlagMemorial.CONTENT_LENGTH_MESSAGE)
        String content
) {}
