package com.example.DunbarHorizon.buzz.application.port.in.command;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;

import java.util.List;

public record CreateBuzzCommand(
        Long creatorId,
        String text,
        List<String> imageUrls,
        RecipientSpec spec
) {}
