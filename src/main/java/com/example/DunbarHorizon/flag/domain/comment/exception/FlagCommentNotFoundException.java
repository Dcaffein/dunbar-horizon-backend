package com.example.DunbarHorizon.flag.domain.comment.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagCommentNotFoundException extends FlagCommentException {
    public FlagCommentNotFoundException(Long id) {
        super(FlagErrorCode.FLAG_COMMENT_NOT_FOUND, "존재하지 않는 flagComment : " + id);
    }
}
