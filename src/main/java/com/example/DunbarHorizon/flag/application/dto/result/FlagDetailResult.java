package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import jakarta.annotation.Nullable;

import java.util.List;

public record FlagDetailResult(
        Long id,
        String title,
        String description,
        int capacity,
        int participantCount,
        @Nullable Long parentFlagId,
        String status,
        FlagScheduleResult schedule,
        FlagHostResult host,
        @Nullable ParentFlagResult parentFlag,
        List<ParticipantResult> participants,
        boolean isHost
) {
    public static FlagDetailResult of(
            Flag flag,
            FlagUserInfo hostInfo,
            @Nullable Flag parentFlag,
            List<ParticipantResult> participants,
            boolean isHost
    ) {
        return new FlagDetailResult(
                flag.getId(),
                flag.getTitle(),
                flag.getDescription(),
                flag.getCapacity(),
                participants.size(),
                flag.getParentId(),
                flag.calculateCurrentStatus().name(),
                FlagScheduleResult.from(flag.getSchedule()),
                FlagHostResult.from(hostInfo),
                parentFlag != null ? ParentFlagResult.from(parentFlag) : null,
                participants,
                isHost
        );
    }
}
