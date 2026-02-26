package com.example.GooRoomBe.flag.domain.memorial.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FlagMemorialException extends BusinessException {
    public FlagMemorialException(String message, HttpStatus httpStatus) {
      super(message, httpStatus);
    }
}
