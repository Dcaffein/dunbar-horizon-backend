package com.example.DunbarHorizon.buzz.domain.event;

public record BuzzThreadOpenedEvent(
        String buzzId,
        Long threadId,
        Long creatorId,
        Long starterId
) {
    public Long notificationReceiverId() {
        return creatorId;
    }

    public Long interactionTargetId() {
        return creatorId;
    }
}
