package com.example.DunbarHorizon.flag.domain.flag.event;

public record FlagEncoreEvent(
        Long parentFlagId,
        Long hostId,
        String title
) {}
