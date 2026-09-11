package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagParticipantNotFoundException extends FlagException{
    public FlagParticipantNotFoundException(Long participantId) {
        super(FlagErrorCode.FLAG_PARTICIPANT_NOT_FOUND, "존재하지 않는 flagParticipant : " + participantId);
    }
}
