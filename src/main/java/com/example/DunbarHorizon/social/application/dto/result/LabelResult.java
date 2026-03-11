package com.example.DunbarHorizon.social.application.dto.result;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.List;

public record LabelResult(
        String id,
        String labelName,
        boolean exposure,
        List<LabelMemberResult> members
) {
    public static LabelResult from(Label label) {
        return new LabelResult(
                label.getId(),
                label.getLabelName(),
                label.isExposure(),
                label.getMembers().stream()
                        .map(LabelMemberResult::from)
                        .toList()
        );
    }
}
