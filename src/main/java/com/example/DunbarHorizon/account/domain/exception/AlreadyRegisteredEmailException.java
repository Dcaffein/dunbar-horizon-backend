package com.example.DunbarHorizon.account.domain.exception;


public class AlreadyRegisteredEmailException extends AccountException {
    public AlreadyRegisteredEmailException(String email)
    {
        super(AccountErrorCode.ACCOUNT_ALREADY_REGISTERED_EMAIL, "이미 인증된 이메일입니다 : " + email);
    }
}
