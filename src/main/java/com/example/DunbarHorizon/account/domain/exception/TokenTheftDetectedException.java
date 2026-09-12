package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class TokenTheftDetectedException extends AccountException {

    public TokenTheftDetectedException(Long userId) {
        super(AccountErrorCode.ACCOUNT_TOKEN_THEFT_DETECTED, ErrorContext.of("userId", userId));
    }
}
