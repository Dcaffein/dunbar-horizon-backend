package com.example.DunbarHorizon.flag.application.port.in.command;

public record FlagCapacityUpdateCommand(
        Long flagId,
        Long hostId,
        Integer capacity
) {}