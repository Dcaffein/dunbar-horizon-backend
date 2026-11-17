package com.example.DunbarHorizon.flag.application.dto.command;

public record FlagDetailsUpdateCommand(
        Long flagId,
        Long hostId,
        String title,
        String description
) {}