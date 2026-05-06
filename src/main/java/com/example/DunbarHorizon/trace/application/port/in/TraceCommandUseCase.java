package com.example.DunbarHorizon.trace.application.port.in;

public interface TraceCommandUseCase {

    void recordTrace(Long visitorId, Long targetId);
}
