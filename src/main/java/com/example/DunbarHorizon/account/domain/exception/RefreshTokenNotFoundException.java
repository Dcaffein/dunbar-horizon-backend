package com.example.DunbarHorizon.account.domain.exception;


public class RefreshTokenNotFoundException extends AccountException {

    public RefreshTokenNotFoundException() {
        super(AccountErrorCode.ACCOUNT_REFRESH_TOKEN_NOT_FOUND);
    }
}
