package com.example.DunbarHorizon.trace.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class TraceException extends BusinessException {
    protected TraceException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
