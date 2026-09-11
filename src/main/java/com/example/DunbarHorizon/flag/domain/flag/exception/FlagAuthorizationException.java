package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagAuthorizationException  extends FlagException {
    public FlagAuthorizationException(String message) {
        super(FlagErrorCode.FLAG_ACCESS_DENIED, message);
    }
}
