package com.example.GooRoomBe.cast.domain.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class CastException extends BusinessException {
    protected CastException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}