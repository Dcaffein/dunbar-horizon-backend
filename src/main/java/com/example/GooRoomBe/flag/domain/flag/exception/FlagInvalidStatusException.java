package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagInvalidStatusException extends FlagException {
    public FlagInvalidStatusException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
