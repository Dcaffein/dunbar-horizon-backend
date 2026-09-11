package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagCommentAuthorizationException extends FlagCommentException {
    public FlagCommentAuthorizationException(String message) {
        super(FlagErrorCode.FLAG_COMMENT_ACCESS_DENIED, message);
    }
}
