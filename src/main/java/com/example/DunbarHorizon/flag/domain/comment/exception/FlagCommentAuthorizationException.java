package com.example.DunbarHorizon.flag.domain.comment.exception;

import org.springframework.http.HttpStatus;

public class FlagCommentAuthorizationException extends FlagCommentException {
    public FlagCommentAuthorizationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
