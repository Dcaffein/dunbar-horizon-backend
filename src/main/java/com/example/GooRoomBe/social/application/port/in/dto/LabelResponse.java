package com.example.GooRoomBe.social.application.port.in.dto;

import com.example.GooRoomBe.social.domain.label.Label;

import java.util.List;

public record LabelResponse(
        String id,
        String labelName,
        boolean exposure,
        List<LabelMemberResponse> members
) {
    public static LabelResponse from(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getLabelName(),
                label.isExposure(),
                label.getMembers().stream()
                        .map(LabelMemberResponse::from)
                        .toList()
        );
    }
}