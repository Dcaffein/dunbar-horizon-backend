package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;

public class LabelConflictException extends LabelException {

    public LabelConflictException(String labelId) {
        super(GlobalErrorCode.CONCURRENT_MODIFICATION, ErrorContext.of("labelId", labelId));
    }
}
