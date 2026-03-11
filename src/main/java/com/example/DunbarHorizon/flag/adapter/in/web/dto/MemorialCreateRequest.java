package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemorialCreateRequest(
        @NotBlank @Size(max = 1000) String content
) {}