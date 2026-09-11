package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class AccountException extends BusinessException {
    protected AccountException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
