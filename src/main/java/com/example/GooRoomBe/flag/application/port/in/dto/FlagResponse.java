package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.domain.flag.Flag;

public record FlagResponse(
        Long id,
        String title,
        String description,
        int capacity,
        String status,
        FlagScheduleResponse schedule,
        FlagHostResponse host
) {
    public static FlagResponse of(Flag flag, FlagUserInfo hostInfo) {
        return new FlagResponse(
                flag.getId(),
                flag.getTitle(),
                flag.getDescription(),
                flag.getCapacity(),
                flag.calculateCurrentStatus().name(),
                FlagScheduleResponse.from(flag.getSchedule()),
                FlagHostResponse.from(hostInfo)
        );
    }
}