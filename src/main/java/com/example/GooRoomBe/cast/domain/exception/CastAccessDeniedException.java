package com.example.GooRoomBe.cast.domain.exception;

import org.springframework.http.HttpStatus;

public class CastAccessDeniedException extends CastException {
    public CastAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}