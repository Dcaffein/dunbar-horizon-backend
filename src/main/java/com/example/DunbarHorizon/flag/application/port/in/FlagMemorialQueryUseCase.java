package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.MemorialResult;

import java.util.List;

public interface FlagMemorialQueryUseCase {
    List<MemorialResult> getMemorials(Long flagId, Long viewerId);
}