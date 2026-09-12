package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagInvitationDuplicateException extends FlagException {

    public FlagInvitationDuplicateException(Long flagId, Long inviteeId) {
        super(FlagErrorCode.FLAG_INVITATION_DUPLICATE, ErrorContext.of("flagId", flagId).and("inviteeId", inviteeId));
    }
}
