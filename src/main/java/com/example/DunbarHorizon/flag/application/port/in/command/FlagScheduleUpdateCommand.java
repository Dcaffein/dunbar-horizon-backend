package com.example.DunbarHorizon.flag.application.port.in.command;

import java.time.LocalDateTime;

public record FlagScheduleUpdateCommand(
        Long flagId,
        Long hostId,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}