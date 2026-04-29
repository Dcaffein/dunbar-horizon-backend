package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuzzCreateRequest(
        @NotBlank(message = "본문 내용은 필수입니다.")
        String text,

        @Valid
        @NotNull(message = "수신자 정보는 필수입니다.")
        RecipientRequest recipient
) {
    public CreateBuzzCommand toCommand(Long creatorId) {
        return new CreateBuzzCommand(creatorId, text, recipient.toSpec());
    }
}
