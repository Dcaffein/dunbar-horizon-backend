package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class BuzzException extends BusinessException {
    protected BuzzException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}