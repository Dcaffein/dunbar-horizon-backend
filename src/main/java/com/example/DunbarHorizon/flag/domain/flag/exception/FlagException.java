package com.example.DunbarHorizon.flag.domain.flag.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class FlagException extends BusinessException {

    protected FlagException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected FlagException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
