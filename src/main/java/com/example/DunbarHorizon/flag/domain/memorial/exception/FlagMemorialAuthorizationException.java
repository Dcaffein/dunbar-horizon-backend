package com.example.DunbarHorizon.flag.domain.memorial.exception;

import org.springframework.http.HttpStatus;

public class FlagMemorialAuthorizationException extends FlagMemorialException {
    public FlagMemorialAuthorizationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
