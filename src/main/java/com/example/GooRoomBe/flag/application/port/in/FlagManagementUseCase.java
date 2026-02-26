package com.example.GooRoomBe.flag.application.port.in;

import com.example.GooRoomBe.flag.application.command.FlagCapacityUpdateCommand;
import com.example.GooRoomBe.flag.application.command.FlagDetailsUpdateCommand;
import com.example.GooRoomBe.flag.application.command.FlagScheduleUpdateCommand;

public interface FlagManagementUseCase {
    void modifyFlagDetails(FlagDetailsUpdateCommand command);
    void modifyFlagCapacity(FlagCapacityUpdateCommand command);
    void reschedule(FlagScheduleUpdateCommand command);
    void closeRecruitment(Long flagId, Long hostId);
    void closeFlag(Long flagId, Long userId);
}