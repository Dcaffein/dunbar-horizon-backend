package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvitationInvalidException extends FlagException {
    public FlagInvitationInvalidException(String message) {
        super(FlagErrorCode.FLAG_INVITATION_INVALID, message);
    }
}
