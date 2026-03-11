package com.example.DunbarHorizon.flag.domain.comment.exception;

import org.springframework.http.HttpStatus;

public class FlagCommentReplyDepthException extends FlagCommentException {
    public FlagCommentReplyDepthException() {
        super("대댓글에는 답글을 달 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
