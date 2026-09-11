package com.example.DunbarHorizon.buzz.domain.exception;


public class BuzzAccessDeniedException extends BuzzException {
    public BuzzAccessDeniedException(String message) {
        super(BuzzErrorCode.BUZZ_ACCESS_DENIED, message);
    }
}