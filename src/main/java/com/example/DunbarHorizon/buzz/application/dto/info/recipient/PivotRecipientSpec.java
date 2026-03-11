package com.example.DunbarHorizon.buzz.application.dto.info.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;

public record PivotRecipientSpec(Long pivotFriendId, Double expansionValue) implements RecipientSpec {
    @Override public RecipientType getType() { return RecipientType.PIVOT; }
}
