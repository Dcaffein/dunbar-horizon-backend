package com.example.DunbarHorizon.global.security.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;

public class ExpiredTokenException extends BusinessException {
    public ExpiredTokenException() {
        super(GlobalErrorCode.AUTH_TOKEN_EXPIRED, "만료된 토큰입니다.");
    }
}
