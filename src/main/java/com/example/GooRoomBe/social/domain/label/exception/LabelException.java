package com.example.GooRoomBe.social.domain.label.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class LabelException extends BusinessException {
    protected LabelException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
