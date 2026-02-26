package com.example.GooRoomBe.cast.domain.exception;

import org.springframework.http.HttpStatus;

public class CastInvalidStateException extends CastException {
    public CastInvalidStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}