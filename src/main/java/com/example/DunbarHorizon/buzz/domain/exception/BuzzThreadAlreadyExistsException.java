package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.global.exception.ErrorContext;

public class BuzzThreadAlreadyExistsException extends BuzzException {

    public BuzzThreadAlreadyExistsException(Long buzzId, Long starterId, ThreadVisibility visibility) {
        super(BuzzErrorCode.BUZZ_THREAD_ALREADY_EXISTS,
                ErrorContext.of("buzzId", buzzId).and("starterId", starterId).and("visibility", visibility));
    }
}
