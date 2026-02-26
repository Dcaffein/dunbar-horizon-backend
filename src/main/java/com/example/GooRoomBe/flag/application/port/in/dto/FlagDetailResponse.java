package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.in.FlagRole;
import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.domain.flag.Flag;

import java.util.List;

public record FlagDetailResponse(
        FlagResponse flag,
        List<ParticipantResponse> participants,
        int participantCount,
        FlagRole role
) {
    public static FlagDetailResponse of(
            Flag flag,
            FlagUserInfo hostInfo,
            List<ParticipantResponse> participants,
            FlagRole role
    ) {
        return new FlagDetailResponse(
                FlagResponse.of(flag, hostInfo),
                participants,
                participants.size(),
                role
        );
    }
}