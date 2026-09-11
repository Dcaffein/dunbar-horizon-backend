package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvalidCapacityException extends FlagException {
    public FlagInvalidCapacityException(String message) {
        super(FlagErrorCode.FLAG_INVALID_CAPACITY, message);
    }
}
