package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.application.dto.result.FlagResult;

import java.util.List;

public interface FlagQueryUseCase {
    List<FlagResult> getFriendFlags(Long userId);
    List<FlagResult> getMyFlagsByRole(Long userId, FlagRole role);
    FlagDetailResult getFlagDetail(Long flagId, Long viewerId);
}