package com.example.GooRoomBe.flag.application.command;

public record FlagCapacityUpdateCommand(
        Long flagId,
        Long hostId,
        Integer capacity
) {}