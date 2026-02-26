package com.example.GooRoomBe.flag.application.port.in;

public interface FlagParticipationUseCase {
    void participateInFlag(Long flagId, Long userId);
    void leaveFlag(Long flagId, Long userId);
}