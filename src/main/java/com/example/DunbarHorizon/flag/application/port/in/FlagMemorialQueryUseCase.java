package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.MemorialListResult;

public interface FlagMemorialQueryUseCase {
    MemorialListResult getMemorials(Long flagId, Long viewerId);
    long getMemorialCount(Long flagId);
}