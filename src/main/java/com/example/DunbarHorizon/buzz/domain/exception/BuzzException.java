package com.example.DunbarHorizon.buzz.domain.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class BuzzException extends BusinessException {

    protected BuzzException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected BuzzException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
