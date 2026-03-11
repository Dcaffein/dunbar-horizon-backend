package com.example.DunbarHorizon.buzz.adapter.out.recipient;

import com.example.DunbarHorizon.buzz.application.dto.info.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ManualRecipientStrategyAdapter implements RecipientStrategyPort {

    private final FriendshipQueryUseCase friendshipQueryUseCase;

    @Override public RecipientType getSupportType() { return RecipientType.MANUAL; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientSpec spec) {
        if (spec instanceof ManualRecipientSpec(var memberIds)) {
            List<Long> filteredIds = memberIds.stream()
                    .filter(id -> !id.equals(creatorId))
                    .toList();
            return friendshipQueryUseCase.getFriendIdsIn(creatorId, filteredIds);
        }
        return Set.of();
    }
}
