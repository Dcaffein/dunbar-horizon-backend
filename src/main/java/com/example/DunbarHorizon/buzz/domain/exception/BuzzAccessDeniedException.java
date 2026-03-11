package com.example.DunbarHorizon.buzz.domain.exception;

import org.springframework.http.HttpStatus;

public class BuzzAccessDeniedException extends BuzzException {
    public BuzzAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}