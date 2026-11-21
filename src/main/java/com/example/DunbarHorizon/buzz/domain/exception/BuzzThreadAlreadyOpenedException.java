package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.global.exception.ErrorContext;

public class BuzzThreadAlreadyOpenedException extends BuzzException {

    public BuzzThreadAlreadyOpenedException(String buzzId, Long starterId, ThreadVisibility visibility) {
        super(BuzzErrorCode.BUZZ_THREAD_ALREADY_OPENED,
                ErrorContext.of("buzzId", buzzId).and("starterId", starterId).and("visibility", visibility));
    }
}
