package com.example.GooRoomBe.account.domain.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class AccountException extends BusinessException {
    protected AccountException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
