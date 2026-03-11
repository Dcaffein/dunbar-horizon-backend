package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import java.time.LocalDateTime;

public record FlagScheduleUpdateRequest(
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) { }
