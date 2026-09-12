package com.example.DunbarHorizon.trace.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class TraceSelfVisitException extends TraceException {

    public TraceSelfVisitException(Long userId) {
        super(TraceErrorCode.TRACE_SELF_VISIT, ErrorContext.of("userId", userId));
    }
}
