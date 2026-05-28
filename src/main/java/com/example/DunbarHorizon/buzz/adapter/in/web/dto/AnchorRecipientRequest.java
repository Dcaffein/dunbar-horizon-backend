package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.AnchorRecipientSpec;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AnchorRecipientRequest(
        @NotNull(message = "기준 친구 ID는 필수입니다.")
        Long anchorFriendId,

        @NotNull(message = "expansionValue는 필수입니다.")
        @DecimalMin(value = "0.0", message = "expansionValue는 0.0 ~ 1.0 사이의 값입니다.")
        @DecimalMax(value = "1.0", message = "expansionValue는 0.0 ~ 1.0 사이의 값입니다.")
        Double expansionValue
) implements RecipientRequest {

    @Override
    public RecipientSpec toSpec() {
        return new AnchorRecipientSpec(anchorFriendId, expansionValue);
    }
}
