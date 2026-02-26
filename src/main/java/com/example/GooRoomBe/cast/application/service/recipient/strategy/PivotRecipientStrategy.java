package com.example.GooRoomBe.cast.application.service.recipient.strategy;

import com.example.GooRoomBe.cast.application.command.recipient.PivotRecipientPayload;
import com.example.GooRoomBe.cast.application.command.recipient.RecipientPayload;
import com.example.GooRoomBe.cast.application.port.out.CastSocialPort;
import com.example.GooRoomBe.cast.application.service.recipient.RecipientStrategy;
import com.example.GooRoomBe.cast.application.service.recipient.RecipientType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PivotRecipientStrategy implements RecipientStrategy {
    private final CastSocialPort castSocialPort;

    @Override public RecipientType getSupportType() { return RecipientType.PIVOT; }

    @Override
    public Set<Long> fetchRecipientIds(Long creatorId, RecipientPayload payload) {
        if (payload instanceof PivotRecipientPayload(Long pivotFriendId, Double expansionValue)) {
            return castSocialPort.getPivotRecipientIds(creatorId, pivotFriendId, expansionValue);
        }
        return Set.of();
    }
}
