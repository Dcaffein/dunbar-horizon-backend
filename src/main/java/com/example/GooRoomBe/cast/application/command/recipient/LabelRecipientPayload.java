package com.example.GooRoomBe.cast.application.command.recipient;

import com.example.GooRoomBe.cast.application.service.recipient.RecipientType;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LabelRecipientPayload(
        @NotEmpty List<String> labelIds
) implements RecipientPayload {
    @Override public RecipientType getType() { return RecipientType.LABEL; }
}
