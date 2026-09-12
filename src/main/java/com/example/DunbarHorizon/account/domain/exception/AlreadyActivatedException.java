package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class AlreadyActivatedException extends AccountException {

    public AlreadyActivatedException(Long userId) {
        super(AccountErrorCode.ACCOUNT_ALREADY_ACTIVATED, ErrorContext.of("userId", userId));
    }
}
