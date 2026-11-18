package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.domain.label.LabelName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCreateRequest(
        @NotBlank(message = "라벨 이름은 필수입니다.")
        @Size(max = LabelName.MAX_LENGTH, message = LabelName.LENGTH_MESSAGE)
        String labelName
) {}
