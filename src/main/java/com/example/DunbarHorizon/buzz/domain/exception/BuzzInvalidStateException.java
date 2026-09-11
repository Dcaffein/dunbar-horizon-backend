package com.example.DunbarHorizon.buzz.domain.exception;


public class BuzzInvalidStateException extends BuzzException {
    public BuzzInvalidStateException(String message) {
        super(BuzzErrorCode.BUZZ_INVALID_STATE, message);
    }
}