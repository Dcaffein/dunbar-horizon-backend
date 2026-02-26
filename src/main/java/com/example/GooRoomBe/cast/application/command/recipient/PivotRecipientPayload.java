package com.example.GooRoomBe.cast.application.command.recipient;

import com.example.GooRoomBe.cast.application.service.recipient.RecipientType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PivotRecipientPayload(
        @NotNull
        Long pivotFriendId,

        @NotNull(message = "expansionValue는 필수입니다.")
        @DecimalMin(value = "0.0", message = "expansionValue는 0.0 ~ 1.0 사이의 값입니다")
        @DecimalMax(value = "1.0", message = "expansionValue는 0.0 ~ 1.0 사이의 값입니다")
        Double expansionValue
) implements RecipientPayload {

    @Override
    public RecipientType getType() {
        return RecipientType.PIVOT;
    }
}
