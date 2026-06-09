package com.example.DunbarHorizon.social.application.dto.result;

import java.util.List;

public record ConnectionPathResult(
        boolean direct,
        List<IntermediaryResult> intermediaries
) {
    public record IntermediaryResult(Long userId, String nickname, Double score) {}
}
