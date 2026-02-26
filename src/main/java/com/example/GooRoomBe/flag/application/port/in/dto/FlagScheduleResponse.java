package com.example.GooRoomBe.flag.application.port.in.dto;

import java.time.LocalDateTime;

record FlagScheduleResponse(
        LocalDateTime deadline,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
    public static FlagScheduleResponse from(com.example.GooRoomBe.flag.domain.flag.FlagSchedule schedule) {
        return new FlagScheduleResponse(
                schedule.getDeadline(),
                schedule.getStartDateTime(),
                schedule.getEndDateTime()
        );
    }
}