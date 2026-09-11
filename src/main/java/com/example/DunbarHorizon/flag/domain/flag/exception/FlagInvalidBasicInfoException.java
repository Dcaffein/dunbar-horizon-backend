package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagInvalidBasicInfoException extends FlagException {
    public FlagInvalidBasicInfoException(String message) {
        super(FlagErrorCode.FLAG_INVALID_BASIC_INFO, message);
    }
}
