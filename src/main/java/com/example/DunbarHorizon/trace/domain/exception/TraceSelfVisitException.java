package com.example.DunbarHorizon.trace.domain.exception;


public class TraceSelfVisitException extends TraceException {
    public TraceSelfVisitException() {
        super(TraceErrorCode.TRACE_SELF_VISIT, "자기 자신을 방문할 수 없습니다.");
    }
}
