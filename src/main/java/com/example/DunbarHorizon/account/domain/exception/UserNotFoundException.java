package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class UserNotFoundException extends AccountException {

    public UserNotFoundException(Long userId) {
        super(AccountErrorCode.ACCOUNT_USER_NOT_FOUND, ErrorContext.of("userId", userId));
    }

    public UserNotFoundException(String email) {
        super(AccountErrorCode.ACCOUNT_USER_NOT_FOUND, ErrorContext.of("email", email));
    }
}
