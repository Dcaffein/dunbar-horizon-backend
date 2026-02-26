package com.example.GooRoomBe.flag.application.command;

import java.time.LocalDateTime;

public record FlagScheduleUpdateCommand(
        Long flagId,
        Long hostId,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}