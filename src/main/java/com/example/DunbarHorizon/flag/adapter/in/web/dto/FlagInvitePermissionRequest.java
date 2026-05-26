package com.example.DunbarHorizon.flag.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record FlagInvitePermissionRequest(
        @NotNull Boolean canInvite
) {}
