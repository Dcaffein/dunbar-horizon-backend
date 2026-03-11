package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FlagMemorialException extends BusinessException {
    public FlagMemorialException(String message, HttpStatus httpStatus) {
      super(message, httpStatus);
    }
}
