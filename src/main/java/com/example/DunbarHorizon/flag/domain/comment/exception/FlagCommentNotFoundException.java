package com.example.DunbarHorizon.flag.domain.comment.exception;

import org.springframework.http.HttpStatus;

public class FlagCommentNotFoundException extends FlagCommentException {
    public FlagCommentNotFoundException(Long id) {
        super("존재하지 않는 flagComment : " + id, HttpStatus.NOT_FOUND);
    }
}
