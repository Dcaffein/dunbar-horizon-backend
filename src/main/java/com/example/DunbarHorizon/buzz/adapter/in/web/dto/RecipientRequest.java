package com.example.DunbarHorizon.buzz.adapter.in.web.dto;

import com.example.DunbarHorizon.buzz.application.dto.info.recipient.RecipientSpec;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LabelRecipientRequest.class, name = "LABEL"),
        @JsonSubTypes.Type(value = ManualRecipientRequest.class, name = "MANUAL"),
        @JsonSubTypes.Type(value = IntermediaryRecipientRequest.class, name = "INTERMEDIARY")
})
public sealed interface RecipientRequest permits LabelRecipientRequest, ManualRecipientRequest, IntermediaryRecipientRequest {
    RecipientSpec toSpec();
}
