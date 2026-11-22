package com.example.DunbarHorizon.buzz.application.dto.command;

import java.util.List;

public record UpdateBuzzCommand(
        Long requesterId,
        Long buzzId,
        String text,
        List<String> imageKeys
) {}
