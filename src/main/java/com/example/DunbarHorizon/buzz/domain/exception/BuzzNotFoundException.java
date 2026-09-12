package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class BuzzNotFoundException extends BuzzException {

    public BuzzNotFoundException(String buzzId) {
        super(BuzzErrorCode.BUZZ_NOT_FOUND, ErrorContext.of("buzzId", buzzId));
    }
}
