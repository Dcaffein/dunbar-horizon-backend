package com.example.DunbarHorizon.flag.domain.memorial.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public class FlagMemorialException extends BusinessException {

    protected FlagMemorialException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected FlagMemorialException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
