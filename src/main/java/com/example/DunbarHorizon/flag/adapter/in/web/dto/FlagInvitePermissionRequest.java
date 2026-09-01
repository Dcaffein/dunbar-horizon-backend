package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record FlagInvitePermissionRequest(
        @NotNull(message = "초대 권한 여부는 필수입니다.") Boolean canInvite
) {}
