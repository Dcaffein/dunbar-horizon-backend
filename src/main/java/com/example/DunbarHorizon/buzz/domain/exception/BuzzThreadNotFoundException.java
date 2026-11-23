package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;

public class BuzzThreadNotFoundException extends BuzzException {

    public BuzzThreadNotFoundException(Long buzzId, Long threadId) {
        super(BuzzErrorCode.BUZZ_THREAD_NOT_FOUND, ErrorContext.of("buzzId", buzzId).and("threadId", threadId));
    }
}
