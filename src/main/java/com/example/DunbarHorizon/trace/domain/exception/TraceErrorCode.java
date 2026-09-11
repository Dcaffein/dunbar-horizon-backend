package com.example.DunbarHorizon.trace.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TraceErrorCode implements ErrorCode {

    TRACE_SELF_VISIT(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
