package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;

public class BuzzCommentNotFoundException extends BuzzException {

    public BuzzCommentNotFoundException(Long threadId, Long commentId) {
        super(BuzzErrorCode.BUZZ_COMMENT_NOT_FOUND, ErrorContext.of("threadId", threadId).and("commentId", commentId));
    }
}
