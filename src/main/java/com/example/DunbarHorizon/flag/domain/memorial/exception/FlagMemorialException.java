package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public class FlagMemorialException extends BusinessException {
    public FlagMemorialException(ErrorCode errorCode, String message) {
      super(errorCode, message);
    }
}
