package com.example.DunbarHorizon.buzz.adapter.out.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.recipient.AnchorRecipientSpec;
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
public class AnchorRecipientStrategyAdapter implements RecipientStrategyPort {

    private final SocialExpansionQueryUseCase expansionQueryUseCase;

    @Override public RecipientType getSupportType() { return RecipientType.ANCHOR; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientSpec spec) {
        if (spec instanceof AnchorRecipientSpec(var anchorFriendId, var expansionValue)) {
            return expansionQueryUseCase.getAnchorExpansion(creatorId, anchorFriendId, expansionValue)
                    .stream()
                    .map(AnchorExpansionResult::id)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
