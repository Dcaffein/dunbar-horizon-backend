package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends AccountException {
    public InvalidVerificationTokenException() {
        super("유효하지 않은 인증 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}