package com.example.DunbarHorizon.flag.domain.flag.event;

import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;

public record FlagDeletedEvent(
        Long flagId,
        Long parentId,
        String flagTitle,
        FlagStatus statusAtDeletion
) {}