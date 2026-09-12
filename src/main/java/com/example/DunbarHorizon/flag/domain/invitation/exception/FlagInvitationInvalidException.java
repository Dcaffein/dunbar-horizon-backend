package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagInvitationInvalidException extends FlagException {

    public FlagInvitationInvalidException(String direction) {
        super(FlagErrorCode.FLAG_INVITATION_INVALID_DIRECTION, ErrorContext.of("direction", direction));
    }
}
