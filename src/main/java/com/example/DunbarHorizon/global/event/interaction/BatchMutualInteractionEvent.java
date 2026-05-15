package com.example.DunbarHorizon.global.event.interaction;

import java.util.List;

public record BatchMutualInteractionEvent(
        List<Long> participantIds,
        Long hostId,
        InteractionType type
) {}
