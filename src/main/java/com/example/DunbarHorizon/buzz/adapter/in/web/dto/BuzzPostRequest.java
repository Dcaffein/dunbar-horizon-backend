package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BuzzPostRequest(
        @NotBlank(message = BuzzComment.TEXT_LENGTH_MESSAGE)
        @Size(max = BuzzComment.TEXT_MAX_LENGTH, message = BuzzComment.TEXT_LENGTH_MESSAGE)
        String text,

        List<String> imageKeys
) {
    public List<String> imageKeysOrEmpty() {
        return imageKeys != null ? imageKeys : List.of();
    }
}
