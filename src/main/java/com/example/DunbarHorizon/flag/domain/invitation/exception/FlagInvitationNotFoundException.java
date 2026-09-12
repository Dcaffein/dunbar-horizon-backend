package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagInvitationNotFoundException extends FlagException {

    public FlagInvitationNotFoundException(Long invitationId) {
        super(FlagErrorCode.FLAG_INVITATION_NOT_FOUND, ErrorContext.of("invitationId", invitationId));
    }
}
