package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class LabelException extends BusinessException {
    protected LabelException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
