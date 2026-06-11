package com.example.DunbarHorizon.social.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCreateRequest(
        @NotBlank(message = "라벨 이름은 필수입니다.")
        @Size(max = 20, message = "라벨 이름은 20자 이내여야 합니다.")
        String labelName
) {}