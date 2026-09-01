package com.example.DunbarHorizon.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagInvalidBasicInfoException extends FlagException {
    public FlagInvalidBasicInfoException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
