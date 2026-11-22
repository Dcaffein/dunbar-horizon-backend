package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommand;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BuzzUpdateRequest(
        @NotBlank(message = "본문 내용은 필수입니다.")
        @Size(max = Buzz.TEXT_MAX_LENGTH, message = Buzz.TEXT_LENGTH_MESSAGE)
        String text,

        List<String> imageKeys
) {
    public UpdateBuzzCommand toCommand(Long requesterId, Long buzzId) {
        return new UpdateBuzzCommand(requesterId, buzzId, text, imageKeys != null ? imageKeys : List.of());
    }
}
