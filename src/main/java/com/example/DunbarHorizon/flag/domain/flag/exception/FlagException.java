package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class FlagException extends BusinessException {
    public FlagException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
