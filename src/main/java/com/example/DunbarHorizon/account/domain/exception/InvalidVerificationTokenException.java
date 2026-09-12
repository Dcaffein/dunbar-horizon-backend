package com.example.DunbarHorizon.account.domain.exception;


public class InvalidVerificationTokenException extends AccountException {

    public InvalidVerificationTokenException() {
        super(AccountErrorCode.ACCOUNT_VERIFICATION_TOKEN_INVALID);
    }
}
