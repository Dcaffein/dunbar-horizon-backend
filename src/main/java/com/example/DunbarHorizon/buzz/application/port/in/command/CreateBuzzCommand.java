package com.example.DunbarHorizon.buzz.application.port.in.command;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;

public record CreateBuzzCommand(
        Long creatorId,
        String text,
        RecipientSpec spec
) {}
