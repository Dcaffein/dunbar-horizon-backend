package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagAuthorizationException  extends FlagException {
    public FlagAuthorizationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
