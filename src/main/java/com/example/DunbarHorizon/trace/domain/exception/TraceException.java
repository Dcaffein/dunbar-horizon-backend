package com.example.DunbarHorizon.trace.domain.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class TraceException extends BusinessException {

    protected TraceException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected TraceException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
