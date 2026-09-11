package com.example.DunbarHorizon.trace.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class TraceException extends BusinessException {
    protected TraceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
