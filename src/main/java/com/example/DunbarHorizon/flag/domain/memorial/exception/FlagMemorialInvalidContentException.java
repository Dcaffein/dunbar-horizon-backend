package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;


public class FlagMemorialInvalidContentException extends FlagMemorialException {

    public FlagMemorialInvalidContentException() {
        super(FlagErrorCode.FLAG_MEMORIAL_INVALID_CONTENT);
    }
}
