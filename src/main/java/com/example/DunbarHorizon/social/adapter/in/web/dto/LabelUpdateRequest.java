package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.domain.label.LabelName;
import jakarta.validation.constraints.Size;

public record LabelUpdateRequest(
        @Size(max = LabelName.MAX_LENGTH, message = LabelName.LENGTH_MESSAGE)
        String labelName
) {}
