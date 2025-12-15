package com.example.GooRoomBe.account.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidJwtException extends AuthException {
    public InvalidJwtException() {
        super("유효하지 않은 jwt입니다.", HttpStatus.UNAUTHORIZED);
    }
}