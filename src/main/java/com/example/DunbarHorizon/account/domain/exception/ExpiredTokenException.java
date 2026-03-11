package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class ExpiredTokenException extends AccountException {
    public ExpiredTokenException() {
        super("만료된 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
}