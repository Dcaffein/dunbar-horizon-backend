package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class AlreadyRegisteredEmailException extends AccountException {

    public AlreadyRegisteredEmailException(String email) {
        super(AccountErrorCode.ACCOUNT_ALREADY_REGISTERED_EMAIL, ErrorContext.of("email", email));
    }
}
