package com.example.DunbarHorizon.social.application.label.dto;

import com.example.DunbarHorizon.social.domain.label.Label;

public record LabelResult(
        String id,
        String labelName,
        int memberCount
) {
    public static LabelResult created(Label label) {
        return new LabelResult(label.getId(), label.getLabelName(), 0);
    }
}
