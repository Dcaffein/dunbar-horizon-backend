package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class BuzzException extends BusinessException {
    protected BuzzException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}