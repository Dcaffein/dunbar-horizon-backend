package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagMemorialAuthorizationException extends FlagMemorialException {
    public FlagMemorialAuthorizationException(String message) {
        super(FlagErrorCode.FLAG_MEMORIAL_ACCESS_DENIED, message);
    }
}
