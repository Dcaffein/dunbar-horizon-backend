package com.example.DunbarHorizon.buzz.domain.event;

import java.util.LinkedHashSet;
import java.util.Set;

public record BuzzCommentedEvent(
        String buzzId,
        Long threadId,
        Long creatorId,
        Long starterId,
        Long authorId
) {
    public Set<Long> notificationReceiverIds() {
        Set<Long> receiverIds = new LinkedHashSet<>();
        receiverIds.add(creatorId);
        receiverIds.add(starterId);
        receiverIds.remove(authorId);
        return receiverIds;
    }

    public Long interactionTargetId() {
        return authorId.equals(starterId) ? creatorId : starterId;
    }
}
