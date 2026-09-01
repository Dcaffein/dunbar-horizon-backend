package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record FlagInviteRequest(
        @NotNull(message = "플래그 정보는 필수입니다.") Long flagId,
        @NotNull(message = "초대할 대상 정보는 필수입니다.") Long inviteeId
) {}
