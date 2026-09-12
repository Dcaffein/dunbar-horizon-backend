package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagCommentAuthorizationException extends FlagCommentException {

    public FlagCommentAuthorizationException(Long commentId, Long userId) {
        super(FlagErrorCode.FLAG_COMMENT_AUTHOR_ONLY, ErrorContext.of("commentId", commentId).and("userId", userId));
    }
}
