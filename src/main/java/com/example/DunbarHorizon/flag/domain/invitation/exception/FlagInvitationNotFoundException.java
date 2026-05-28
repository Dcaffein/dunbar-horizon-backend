package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import org.springframework.http.HttpStatus;

public class FlagInvitationNotFoundException extends FlagException {
    public FlagInvitationNotFoundException(Long invitationId) {
        super("존재하지 않는 초대장: " + invitationId, HttpStatus.NOT_FOUND);
    }
}
