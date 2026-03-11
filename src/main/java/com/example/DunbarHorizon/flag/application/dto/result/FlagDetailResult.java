package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.port.in.FlagRole;
import com.example.DunbarHorizon.flag.domain.flag.Flag;

import java.util.List;

public record FlagDetailResult(
        FlagResult flag,
        List<ParticipantResult> participants,
        int participantCount,
        FlagRole role
) {
    public static FlagDetailResult of(
            Flag flag,
            FlagUserInfo hostInfo,
            List<ParticipantResult> participants,
            FlagRole role
    ) {
        return new FlagDetailResult(
                FlagResult.of(flag, hostInfo),
                participants,
                participants.size(),
                role
        );
    }
}
