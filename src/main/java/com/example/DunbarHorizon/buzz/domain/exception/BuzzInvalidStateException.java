package com.example.DunbarHorizon.buzz.domain.exception;



import com.example.DunbarHorizon.global.exception.ErrorContext;
/**
 * 조건마다 코드가 다르다. 호출부가 BuzzErrorCode의 상수를 지정한다.
 */
public class BuzzInvalidStateException extends BuzzException {

    public BuzzInvalidStateException(BuzzErrorCode errorCode) {
        super(errorCode);
    }

    public BuzzInvalidStateException(BuzzErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
