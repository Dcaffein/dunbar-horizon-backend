package com.example.DunbarHorizon.flag.application.port.in.command;

public record FlagDetailsUpdateCommand(
        Long flagId,
        Long hostId,
        String title,
        String description
) {}