package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.flag.Flag;

public record FlagResult(
        Long id,
        String title,
        String description,
        int capacity,
        int participantCount,
        String status,
        FlagScheduleResult schedule,
        FlagHostResult host
) {
    public static FlagResult of(Flag flag, FlagUserInfo hostInfo, int participantCount) {
        return new FlagResult(
                flag.getId(),
                flag.getTitle(),
                flag.getDescription(),
                flag.getCapacity(),
                participantCount,
                flag.calculateCurrentStatus().name(),
                FlagScheduleResult.from(flag.getSchedule()),
                FlagHostResult.from(hostInfo)
        );
    }
}
