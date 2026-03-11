package com.example.DunbarHorizon.buzz.application.dto.info.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;

import java.util.List;

public record LabelRecipientSpec(List<String> labelIds) implements RecipientSpec {
    @Override public RecipientType getType() { return RecipientType.LABEL; }
}
