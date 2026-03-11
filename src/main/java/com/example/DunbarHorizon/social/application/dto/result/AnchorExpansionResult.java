package com.example.DunbarHorizon.social.application.dto.result;

public record AnchorExpansionResult(
        Long id,
        String nickname,
        Double intimacy,
        Integer mutualCount,
        Integer labelCount
) {}