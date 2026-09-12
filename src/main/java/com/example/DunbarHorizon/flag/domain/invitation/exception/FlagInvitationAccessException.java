package com.example.DunbarHorizon.flag.domain.invitation.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;



/**
 * 조건마다 코드가 다르다. 호출부가 FlagErrorCode의 상수를 지정한다.
 */
public class FlagInvitationAccessException extends FlagException {

    public FlagInvitationAccessException(FlagErrorCode errorCode) {
        super(errorCode);
    }

    public FlagInvitationAccessException(FlagErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
