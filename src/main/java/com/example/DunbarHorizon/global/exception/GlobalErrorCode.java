package com.example.DunbarHorizon.global.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인에 속하지 않는 에러 코드. 스프링이 요청 처리 전에 던지는 웹 예외와 두 출구의 기본값이다.
 *
 * <p>경로나 메서드처럼 요청에서 온 값은 문장에 넣지 않는다. 그대로 되비추면 공격자가 넣은
 * 문자열이 응답에 실려 나가는 통로가 된다. 그 값들은 로그에만 남긴다.
 */
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "요청 파라미터의 형식이 올바르지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 요청과 충돌이 발생했습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

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
