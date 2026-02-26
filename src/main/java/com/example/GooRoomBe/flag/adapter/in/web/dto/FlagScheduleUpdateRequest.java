package com.example.GooRoomBe.flag.adapter.in.web.dto;

import java.time.LocalDateTime;

public record FlagScheduleUpdateRequest(
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) { }
