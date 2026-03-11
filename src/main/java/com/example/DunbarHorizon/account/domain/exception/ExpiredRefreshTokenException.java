package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class ExpiredRefreshTokenException extends AccountException{
    public ExpiredRefreshTokenException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
