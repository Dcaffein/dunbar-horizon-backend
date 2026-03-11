package com.example.DunbarHorizon.social.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LabelMembersReplaceRequest(
        @NotNull(message = "멤버 ID 리스트는 필수입니다. (빈 리스트 허용)")
        List<Long> memberIds
) {}