package com.example.DunbarHorizon.buzz.application.dto.command;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;

public record CreateBuzzCommand(
        Long creatorId,
        String text,
        RecipientSpec spec
) {}
