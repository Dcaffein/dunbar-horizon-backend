package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagNotFoundException extends FlagException {

    public FlagNotFoundException(Long flagId) {
        super(FlagErrorCode.FLAG_NOT_FOUND, ErrorContext.of("flagId", flagId));
    }
}
