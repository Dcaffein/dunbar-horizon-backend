package com.example.DunbarHorizon.flag.application.port.in.command;

import java.time.LocalDateTime;

public record FlagEncoreCommand(
        Long parentFlagId,
        Long hostId,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}