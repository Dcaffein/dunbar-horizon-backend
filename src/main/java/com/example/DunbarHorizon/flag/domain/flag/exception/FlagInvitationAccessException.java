package com.example.DunbarHorizon.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagInvitationAccessException extends FlagException {
    public FlagInvitationAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
