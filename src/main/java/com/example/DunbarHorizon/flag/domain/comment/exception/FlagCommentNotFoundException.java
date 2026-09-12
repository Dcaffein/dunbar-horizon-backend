package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagCommentNotFoundException extends FlagCommentException {

    public FlagCommentNotFoundException(Long commentId) {
        super(FlagErrorCode.FLAG_COMMENT_NOT_FOUND, ErrorContext.of("commentId", commentId));
    }
}
