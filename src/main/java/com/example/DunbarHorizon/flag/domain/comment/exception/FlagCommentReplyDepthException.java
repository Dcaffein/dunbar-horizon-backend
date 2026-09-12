package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagCommentReplyDepthException extends FlagCommentException {

    public FlagCommentReplyDepthException(Long parentCommentId) {
        super(FlagErrorCode.FLAG_COMMENT_REPLY_DEPTH_EXCEEDED, ErrorContext.of("parentCommentId", parentCommentId));
    }
}
