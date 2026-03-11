package com.example.DunbarHorizon.buzz.adapter.out.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.recipient.PivotRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.in.SocialExpansionQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PivotRecipientStrategyAdapter implements RecipientStrategyPort {

    private final SocialExpansionQueryUseCase expansionQueryUseCase;

    @Override public RecipientType getSupportType() { return RecipientType.PIVOT; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientSpec spec) {
        if (spec instanceof PivotRecipientSpec(var pivotFriendId, var expansionValue)) {
            return expansionQueryUseCase.getAnchorExpansion(creatorId, pivotFriendId, expansionValue)
                    .stream()
                    .map(AnchorExpansionResult::id)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
