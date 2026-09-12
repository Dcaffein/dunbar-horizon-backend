package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagFullCapacityException extends FlagException {

    public FlagFullCapacityException(Long flagId, int capacity, int participantCount) {
        super(FlagErrorCode.FLAG_FULL_CAPACITY,
                ErrorContext.of("flagId", flagId).and("capacity", capacity).and("participantCount", participantCount));
    }
}
