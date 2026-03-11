package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class VerificationTokenNotFoundException extends AccountException {
    public VerificationTokenNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
