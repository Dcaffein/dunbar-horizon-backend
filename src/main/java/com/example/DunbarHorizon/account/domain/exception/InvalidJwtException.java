package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class InvalidJwtException extends AccountException {
    public InvalidJwtException() {
        super("유효하지 않은 jwt입니다.", HttpStatus.UNAUTHORIZED);
    }
}