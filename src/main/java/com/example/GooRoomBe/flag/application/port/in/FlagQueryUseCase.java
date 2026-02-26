package com.example.GooRoomBe.flag.application.port.in;

import com.example.GooRoomBe.flag.application.port.in.dto.FlagDetailResponse;
import com.example.GooRoomBe.flag.application.port.in.dto.FlagResponse;

import java.util.List;

public interface FlagQueryUseCase {
    List<FlagResponse> getFriendFlags(Long userId);
    List<FlagResponse> getMyFlagsByRole(Long userId, FlagRole role);
    FlagDetailResponse getFlagDetail(Long flagId, Long viewerId);
}