package com.example.DunbarHorizon.trace.application.port.in;

import com.example.DunbarHorizon.trace.application.dto.TraceResult;

public interface TraceCommandUseCase {

    TraceResult recordTrace(Long visitorId, Long targetId);
}
