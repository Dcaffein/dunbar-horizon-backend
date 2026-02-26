package com.example.GooRoomBe.cast.application.service.recipient.strategy;

import com.example.GooRoomBe.cast.application.command.recipient.ManualRecipientPayload;
import com.example.GooRoomBe.cast.application.command.recipient.RecipientPayload;
import com.example.GooRoomBe.cast.application.port.out.CastSocialPort;
import com.example.GooRoomBe.cast.application.service.recipient.RecipientStrategy;
import com.example.GooRoomBe.cast.application.service.recipient.RecipientType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ManualRecipientStrategy implements RecipientStrategy {
    private final CastSocialPort castSocialPort;

    @Override public RecipientType getSupportType() { return RecipientType.MANUAL; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientPayload payload) {
        if (payload instanceof ManualRecipientPayload(List<Long> memberIds)) {
            List<Long> filteredIds = memberIds.stream()
                    .filter(id -> !id.equals(creatorId))
                    .toList();
            return castSocialPort.filterOnlyFriends(creatorId, filteredIds);
        }
        return Set.of();
    }
}
