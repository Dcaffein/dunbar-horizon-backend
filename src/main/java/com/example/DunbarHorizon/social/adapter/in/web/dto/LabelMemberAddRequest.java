package com.example.DunbarHorizon.social.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record LabelMemberAddRequest(
        @NotNull(message = "추가할 멤버 ID는 필수입니다.")
        Long memberId
) {}