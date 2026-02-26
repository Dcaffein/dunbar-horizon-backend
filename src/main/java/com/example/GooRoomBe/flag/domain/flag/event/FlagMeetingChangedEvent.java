package com.example.GooRoomBe.flag.domain.flag.event;

import java.time.LocalDateTime;

public record FlagMeetingChangedEvent(
        Long flagId,
        String flagTitle,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
}
