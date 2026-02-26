package com.example.GooRoomBe.cast.adapter.in.web.dto;

import com.example.GooRoomBe.cast.application.command.CreateCastCommand;
import com.example.GooRoomBe.cast.application.command.recipient.RecipientPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CastCreateRequestDto(
        @NotBlank(message = "본문 내용은 필수입니다.")
        String text,

        List<String> imageUrls,

        @NotNull(message = "수신자 지정 정보(payload)는 필수입니다.")
        @Valid
        RecipientPayload payload
) {
    public CreateCastCommand toCommand(Long creatorId) {
        return new CreateCastCommand(
                creatorId,
                this.text,
                this.imageUrls,
                this.payload
        );
    }
}