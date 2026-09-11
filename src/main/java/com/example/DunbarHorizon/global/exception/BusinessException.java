package com.example.DunbarHorizon.global.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getCode() {
        return errorCode.code();
    }

    public HttpStatus getHttpStatus() {
        return errorCode.status();
    }
}
