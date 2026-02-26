package com.example.GooRoomBe.flag.domain.flag.event;

public record FlagEncoreEvent(
        Long parentFlagId,
        Long hostId,
        String title
) {}
