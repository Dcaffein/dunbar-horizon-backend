package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum BuzzErrorCode implements ErrorCode {

    BUZZ_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    BUZZ_INVALID_STATE(HttpStatus.BAD_REQUEST),
    BUZZ_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus status;

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
