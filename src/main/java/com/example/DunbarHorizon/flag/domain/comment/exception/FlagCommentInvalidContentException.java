package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;


public class FlagCommentInvalidContentException extends FlagCommentException {

    public FlagCommentInvalidContentException() {
        super(FlagErrorCode.FLAG_COMMENT_INVALID_CONTENT);
    }
}
