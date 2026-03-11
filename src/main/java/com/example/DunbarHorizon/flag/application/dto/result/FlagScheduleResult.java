package com.example.DunbarHorizon.flag.application.dto.result;

import java.time.LocalDateTime;

record FlagScheduleResult(
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
    public static FlagScheduleResult from(com.example.DunbarHorizon.flag.domain.flag.FlagSchedule schedule) {
        return new FlagScheduleResult(
                schedule.getDeadline(),
                schedule.getStartDateTime(),
                schedule.getEndDateTime()
        );
    }
}
