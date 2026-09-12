package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;

/**
 * 이전에는 {@code BuzzInvalidStateException}으로 던져 400이 나갔다. 찾지 못한 것이므로 404다.
 * 굵은 코드에 여러 조건을 몰아넣은 탓에 드러나지 않았던 분류 오류다.
 */
public class BuzzCommentNotFoundException extends BuzzException {

    public BuzzCommentNotFoundException(String buzzId, String commentId) {
        super(BuzzErrorCode.BUZZ_COMMENT_NOT_FOUND, ErrorContext.of("buzzId", buzzId).and("commentId", commentId));
    }
}
