package com.example.DunbarHorizon.buzz.domain.exception;



import com.example.DunbarHorizon.global.exception.ErrorContext;
/**
 * 조건마다 코드가 다르다. 호출부가 BuzzErrorCode의 상수를 지정한다.
 */
public class BuzzAccessDeniedException extends BuzzException {

    public BuzzAccessDeniedException(BuzzErrorCode errorCode) {
        super(errorCode);
    }

    public BuzzAccessDeniedException(BuzzErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
