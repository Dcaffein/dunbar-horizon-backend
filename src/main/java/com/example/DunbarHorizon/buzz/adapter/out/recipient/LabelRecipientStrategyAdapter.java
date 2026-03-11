package com.example.DunbarHorizon.buzz.adapter.out.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.recipient.LabelRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import com.example.DunbarHorizon.social.application.port.in.LabelQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class LabelRecipientStrategyAdapter implements RecipientStrategyPort {
    private final LabelQueryUseCase labelQueryUseCase;

    @Override public RecipientType getSupportType() { return RecipientType.LABEL; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientSpec spec) {
        if (spec instanceof LabelRecipientSpec(var labelIds)) {
            return labelQueryUseCase.getMemberIdsByLabels(creatorId, labelIds);
        }
        return Set.of();
    }
}
