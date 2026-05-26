package com.example.DunbarHorizon.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagInvitationExpiredException extends FlagException {
    public FlagInvitationExpiredException() {
        super("만료된 초대장입니다.", HttpStatus.BAD_REQUEST);
    }
}
