package com.example.DunbarHorizon.account.domain.exception;


public class AlreadyActivatedException extends AccountException{
    public AlreadyActivatedException(Long userId) {
        super(AccountErrorCode.ACCOUNT_ALREADY_ACTIVATED, "not unverified user : " + userId);
    }
}
