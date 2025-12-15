package com.example.GooRoomBe.account.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends AuthException {
    public InvalidVerificationTokenException() {
        super("유효하지 않은 인증 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}