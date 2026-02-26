package com.example.GooRoomBe.flag.application.command;

public record FlagDetailsUpdateCommand(
        Long flagId,
        Long hostId,
        String title,
        String description
) {}