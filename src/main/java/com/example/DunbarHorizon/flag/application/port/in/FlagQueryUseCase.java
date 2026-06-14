package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.application.dto.result.FlagResult;

import java.util.List;

public interface FlagQueryUseCase {
    List<FlagResult> getFriendFlags(Long userId);
    List<FlagResult> getFlagsByRole(Long userId, FlagRole role);
    List<FlagResult> getRecentFlags(Long userId);
    FlagDetailResult getFlagDetail(Long flagId, Long viewerId);
}