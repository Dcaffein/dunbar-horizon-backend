package com.example.DunbarHorizon.flag.domain.flag.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;



/**
 * 조건마다 코드가 다르다. 호출부가 FlagErrorCode의 상수를 지정한다.
 */
public class FlagScheduleInvalidException extends FlagException {

    public FlagScheduleInvalidException(FlagErrorCode errorCode) {
        super(errorCode);
    }

    public FlagScheduleInvalidException(FlagErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
