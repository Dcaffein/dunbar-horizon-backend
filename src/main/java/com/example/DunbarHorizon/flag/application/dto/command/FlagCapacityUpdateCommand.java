package com.example.DunbarHorizon.flag.application.dto.command;

public record FlagCapacityUpdateCommand(
        Long flagId,
        Long hostId,
        Integer capacity
) {}