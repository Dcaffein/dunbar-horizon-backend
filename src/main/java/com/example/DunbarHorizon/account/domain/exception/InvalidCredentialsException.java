package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class InvalidCredentialsException extends AccountException {

    public InvalidCredentialsException(String email) {
        super(AccountErrorCode.ACCOUNT_INVALID_CREDENTIALS, ErrorContext.of("email", email));
    }
}
