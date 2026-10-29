package com.example.DunbarHorizon.flag.domain.flag.event;

public record FlagExpiryExemptionChangedEvent(
        Long flagId,
        Long hostId,
        Long parentId,
        boolean exempt
) {}
