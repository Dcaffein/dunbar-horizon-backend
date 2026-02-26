package com.example.GooRoomBe.flag.domain.flag.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class FlagException extends BusinessException {
    public FlagException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
