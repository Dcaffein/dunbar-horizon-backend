package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.domain.label.Label;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCreateRequest(
        @NotBlank(message = "라벨 이름은 필수입니다.")
        @Size(max = Label.NAME_MAX_LENGTH, message = Label.NAME_LENGTH_MESSAGE)
        String labelName
) {}