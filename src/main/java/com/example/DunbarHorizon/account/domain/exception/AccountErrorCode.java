package com.example.DunbarHorizon.account.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ACCOUNT_ALREADY_ACTIVATED(HttpStatus.CONFLICT, "이미 활성화된 계정입니다."),
    ACCOUNT_ALREADY_REGISTERED_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ACCOUNT_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "로그인 정보가 없습니다. 다시 로그인해주세요."),
    ACCOUNT_VERIFICATION_TOKEN_INVALID(HttpStatus.GONE, "유효하지 않거나 만료된 인증 링크입니다."),
    ACCOUNT_TOKEN_THEFT_DETECTED(HttpStatus.FORBIDDEN, "보안 위협이 감지되었습니다. 안전을 위해 다시 로그인해주세요.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
