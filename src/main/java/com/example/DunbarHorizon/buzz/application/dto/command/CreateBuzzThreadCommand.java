package com.example.DunbarHorizon.buzz.application.dto.command;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.util.List;

public record CreateBuzzThreadCommand(
        Long starterId,
        Long buzzId,
        ThreadVisibility visibility,
        String text,
        List<String> imageKeys
) {}
