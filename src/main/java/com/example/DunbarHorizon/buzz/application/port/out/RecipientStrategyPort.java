package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;

import java.util.Set;

public interface RecipientStrategyPort {
    RecipientType getSupportType();
    Set<Long> fetchRecipientIds(Long creatorId, RecipientSpec spec);
}
