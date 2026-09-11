package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagCommentReplyDepthException extends FlagCommentException {
    public FlagCommentReplyDepthException() {
        super(FlagErrorCode.FLAG_COMMENT_REPLY_DEPTH_EXCEEDED, "대댓글에는 답글을 달 수 없습니다.");
    }
}
