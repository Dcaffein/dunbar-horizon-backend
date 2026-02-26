package com.example.GooRoomBe.flag.domain.comment.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class FlagCommentException extends BusinessException {
    public FlagCommentException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
