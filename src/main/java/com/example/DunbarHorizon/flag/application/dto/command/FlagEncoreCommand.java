package com.example.DunbarHorizon.flag.application.dto.command;

import java.time.LocalDateTime;

public record FlagEncoreCommand(
        Long parentFlagId,
        Long hostId,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}