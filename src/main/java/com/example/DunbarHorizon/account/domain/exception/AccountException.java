package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class AccountException extends BusinessException {
    protected AccountException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
