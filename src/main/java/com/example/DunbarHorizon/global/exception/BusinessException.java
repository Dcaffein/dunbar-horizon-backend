package com.example.DunbarHorizon.global.exception;

import org.springframework.http.HttpStatus;


/**
 * 도메인이 던지는 예외의 뿌리.
 *
 * <p>문구가 두 청중을 겸하지 않게 나눈다.
 *
 * <pre>
 * 응답   getUserMessage()  요청하신 깃발을 찾을 수 없습니다.
 * 로그   getMessage()      FLAG_NOT_FOUND {flagId=99999999}
 * </pre>
 *
 * <p>로그에는 문장을 싣지 않는다. 코드가 이미 조건을 정확히 말하므로 문장은 같은 말의
 * 반복이고, 한글 문장은 콘솔 코드페이지나 로그 수집기를 거치며 깨질 수 있다.
 *
 * <p>조립을 생성자에서 하므로 잡히지 않고 올라간 경우에도 스택트레이스에 컨텍스트가 남는다.
 */
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        this(errorCode, ErrorContext.none());
    }

    protected BusinessException(ErrorCode errorCode, ErrorContext context) {
        super(compose(errorCode, context));
        this.errorCode = errorCode;
    }

    public String getCode() {
        return errorCode.code();
    }

    public HttpStatus getHttpStatus() {
        return errorCode.status();
    }

    /** 응답 본문에 실을 문장. */
    public String getUserMessage() {
        return errorCode.message();
    }

    private static String compose(ErrorCode errorCode, ErrorContext context) {
        if (context == null || context.isEmpty()) {
            return errorCode.code();
        }
        return errorCode.code() + " " + context;
    }
}
