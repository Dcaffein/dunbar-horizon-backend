package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BuzzThreadCreateRequest(
        @NotBlank(message = BuzzThread.TEXT_LENGTH_MESSAGE)
        @Size(max = BuzzThread.TEXT_MAX_LENGTH, message = BuzzThread.TEXT_LENGTH_MESSAGE)
        String text,

        @NotNull(message = "스레드 공개 범위는 필수입니다.")
        ThreadVisibility visibility,

        List<String> imageKeys
) {
    public CreateBuzzThreadCommand toCommand(Long starterId, Long buzzId) {
        return new CreateBuzzThreadCommand(starterId, buzzId, visibility, text,
                imageKeys != null ? imageKeys : List.of());
    }
}
