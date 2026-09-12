package com.example.DunbarHorizon.social.domain.label.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class LabelException extends BusinessException {

    protected LabelException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected LabelException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
