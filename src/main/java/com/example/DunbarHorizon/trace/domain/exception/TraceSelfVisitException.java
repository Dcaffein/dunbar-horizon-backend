package com.example.DunbarHorizon.trace.domain.exception;

import org.springframework.http.HttpStatus;

public class TraceSelfVisitException extends TraceException {
    public TraceSelfVisitException() {
        super("자기 자신을 방문할 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
