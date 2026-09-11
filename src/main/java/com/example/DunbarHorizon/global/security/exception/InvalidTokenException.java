package com.example.DunbarHorizon.global.security.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException() {
        super(GlobalErrorCode.AUTH_TOKEN_INVALID, "유효하지 않은 토큰입니다.");
    }
}
