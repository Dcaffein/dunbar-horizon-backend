package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.LabelRecipientSpec;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LabelRecipientRequest(
        @NotEmpty(message = "라벨 ID는 하나 이상 필요합니다.")
        List<String> labelIds
) implements RecipientRequest {

    @Override
    public RecipientSpec toSpec() {
        return new LabelRecipientSpec(labelIds);
    }
}
