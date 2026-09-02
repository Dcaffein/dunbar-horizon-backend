package com.example.DunbarHorizon.social.adapter.in.web.dto;

import com.example.DunbarHorizon.social.domain.label.Label;
import jakarta.validation.constraints.Size;

public record LabelUpdateRequest(
        @Size(max = Label.NAME_MAX_LENGTH, message = Label.NAME_LENGTH_MESSAGE)
        String labelName
) {}