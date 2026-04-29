package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.ManualRecipientSpec;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ManualRecipientRequest(
        @NotEmpty(message = "수신자 ID는 하나 이상 필요합니다.")
        @Size(max = 150, message = "수신자는 최대 150명까지 지정할 수 있습니다.")
        List<Long> memberIds
) implements RecipientRequest {

    @Override
    public RecipientSpec toSpec() {
        return new ManualRecipientSpec(memberIds);
    }
}
