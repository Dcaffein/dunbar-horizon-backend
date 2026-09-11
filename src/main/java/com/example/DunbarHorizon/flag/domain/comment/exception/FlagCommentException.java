package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class FlagCommentException extends BusinessException {
    public FlagCommentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
