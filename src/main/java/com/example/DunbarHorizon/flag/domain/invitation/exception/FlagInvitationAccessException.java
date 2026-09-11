package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvitationAccessException extends FlagException {
    public FlagInvitationAccessException(String message) {
        super(FlagErrorCode.FLAG_INVITATION_ACCESS_DENIED, message);
    }
}
