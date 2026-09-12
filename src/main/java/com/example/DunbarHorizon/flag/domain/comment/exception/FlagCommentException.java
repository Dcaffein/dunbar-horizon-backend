package com.example.DunbarHorizon.flag.domain.comment.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class FlagCommentException extends BusinessException {

    protected FlagCommentException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected FlagCommentException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
