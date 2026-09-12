package com.example.DunbarHorizon.account.domain.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class AccountException extends BusinessException {

    protected AccountException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected AccountException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
