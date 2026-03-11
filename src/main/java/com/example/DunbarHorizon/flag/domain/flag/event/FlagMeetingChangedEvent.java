package com.example.DunbarHorizon.flag.domain.flag.event;

import java.time.LocalDateTime;

public record FlagMeetingChangedEvent(
        Long flagId,
        String flagTitle,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
}
