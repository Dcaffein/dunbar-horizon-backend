package com.example.DunbarHorizon.flag.application.dto.command;


import java.time.LocalDateTime;

public record FlagHostCommand(
        Long hostId,
        String title,
        String description,
        Integer capacity,
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {}