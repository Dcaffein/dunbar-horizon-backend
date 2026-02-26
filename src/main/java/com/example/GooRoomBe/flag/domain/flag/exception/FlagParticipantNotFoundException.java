package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagParticipantNotFoundException extends FlagException{
    public FlagParticipantNotFoundException(Long participantId) {
        super("존재하지 않는 flagParticipant : " + participantId, HttpStatus.NOT_FOUND);
    }
}
