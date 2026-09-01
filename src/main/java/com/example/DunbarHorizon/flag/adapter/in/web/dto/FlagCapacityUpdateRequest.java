package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.Min;

public record FlagCapacityUpdateRequest(
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다.") Integer capacity
) {}
