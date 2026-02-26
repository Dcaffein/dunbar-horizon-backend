package com.example.GooRoomBe.flag.domain.flag.event;

import com.example.GooRoomBe.flag.domain.flag.FlagStatus;

public record FlagDeletedEvent(
        Long flagId,
        Long parentId,
        String flagTitle,
        FlagStatus statusAtDeletion
) {}