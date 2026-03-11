package com.example.DunbarHorizon.buzz.domain.exception;

import org.springframework.http.HttpStatus;

public class BuzzInvalidStateException extends BuzzException {
    public BuzzInvalidStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}