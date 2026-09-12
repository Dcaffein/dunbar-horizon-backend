package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagInvalidCapacityException extends FlagException {

    public FlagInvalidCapacityException(Integer capacity) {
        super(FlagErrorCode.FLAG_INVALID_CAPACITY, ErrorContext.of("capacity", capacity));
    }
}
