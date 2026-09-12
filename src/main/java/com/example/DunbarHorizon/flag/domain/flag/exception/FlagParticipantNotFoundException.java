package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagParticipantNotFoundException extends FlagException {

    public FlagParticipantNotFoundException(Long participantId) {
        super(FlagErrorCode.FLAG_PARTICIPANT_NOT_FOUND, ErrorContext.of("participantId", participantId));
    }
}
