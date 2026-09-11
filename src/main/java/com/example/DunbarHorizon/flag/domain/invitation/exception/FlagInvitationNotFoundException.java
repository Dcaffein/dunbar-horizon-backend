package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvitationNotFoundException extends FlagException {
    public FlagInvitationNotFoundException(Long invitationId) {
        super(FlagErrorCode.FLAG_INVITATION_NOT_FOUND, "존재하지 않는 초대장: " + invitationId);
    }
}
