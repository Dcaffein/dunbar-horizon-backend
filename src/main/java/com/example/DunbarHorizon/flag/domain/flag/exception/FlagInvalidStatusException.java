package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvalidStatusException extends FlagException {
    public FlagInvalidStatusException(String message) {
        super(FlagErrorCode.FLAG_INVALID_STATUS, message);
    }
}
