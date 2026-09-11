package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_ALREADY_ACTIVATED(HttpStatus.CONFLICT),
    ACCOUNT_ALREADY_REGISTERED_EMAIL(HttpStatus.CONFLICT),
    ACCOUNT_AUTH_NOT_FOUND(HttpStatus.NOT_FOUND),
    ACCOUNT_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCOUNT_VERIFICATION_TOKEN_INVALID(HttpStatus.GONE),
    ACCOUNT_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED),
    ACCOUNT_TOKEN_THEFT_DETECTED(HttpStatus.FORBIDDEN),
    ACCOUNT_USER_NOT_FOUND(HttpStatus.NOT_FOUND);

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
