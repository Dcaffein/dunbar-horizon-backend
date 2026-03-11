package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class FlagCommentException extends BusinessException {
    public FlagCommentException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
