package com.example.DunbarHorizon.flag.domain.invitation.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagException;
import org.springframework.http.HttpStatus;

public class FlagInvitationDuplicateException extends FlagException {
    public FlagInvitationDuplicateException(Long flagId, Long inviteeId) {
        super(String.format("이미 대기 중인 초대장이 존재합니다. flagId=%d, inviteeId=%d", flagId, inviteeId), HttpStatus.CONFLICT);
    }
}
