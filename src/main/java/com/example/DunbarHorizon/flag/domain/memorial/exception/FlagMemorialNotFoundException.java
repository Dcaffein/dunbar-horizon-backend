package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagMemorialNotFoundException extends FlagMemorialException {

    public FlagMemorialNotFoundException(Long memorialId) {
        super(FlagErrorCode.FLAG_MEMORIAL_NOT_FOUND, ErrorContext.of("memorialId", memorialId));
    }
}
