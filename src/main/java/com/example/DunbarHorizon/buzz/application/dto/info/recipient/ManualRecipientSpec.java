package com.example.DunbarHorizon.buzz.application.dto.info.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;

import java.util.List;

public record ManualRecipientSpec(List<Long> memberIds) implements RecipientSpec {
    @Override public RecipientType getType() { return RecipientType.MANUAL; }
}
