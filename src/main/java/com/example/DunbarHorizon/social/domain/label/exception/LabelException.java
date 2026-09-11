package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class LabelException extends BusinessException {
    protected LabelException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
