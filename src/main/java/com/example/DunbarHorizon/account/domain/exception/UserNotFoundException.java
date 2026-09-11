package com.example.DunbarHorizon.account.domain.exception;


public class UserNotFoundException extends AccountException {
    public UserNotFoundException(String message) {
        super(AccountErrorCode.ACCOUNT_USER_NOT_FOUND, message);
    }
}
