package com.example.GooRoomBe.flag.application.command;

import java.time.LocalDateTime;

public record FlagEncoreCommand(
        Long parentFlagId,
        Long hostId,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}