package com.example.GooRoomBe.cast.application.command.recipient;

import com.example.GooRoomBe.cast.application.service.recipient.RecipientType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LabelRecipientPayload.class, name = "LABEL"),
        @JsonSubTypes.Type(value = ManualRecipientPayload.class, name = "MANUAL"),
        @JsonSubTypes.Type(value = PivotRecipientPayload.class, name = "PIVOT")
})
public interface RecipientPayload {
    RecipientType getType();
}
