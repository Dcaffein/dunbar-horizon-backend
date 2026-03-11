package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class AlreadyRegisteredEmailException extends AccountException {
    public AlreadyRegisteredEmailException(String email)
    {
        super("이미 인증된 이메일입니다 : " + email, HttpStatus.CONFLICT);
    }
}
