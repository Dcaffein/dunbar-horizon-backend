package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.Min;

public record FlagCapacityUpdateRequest(
        @Min(1) Integer capacity
) {}