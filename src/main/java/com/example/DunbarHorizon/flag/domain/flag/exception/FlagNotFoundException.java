package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagNotFoundException extends FlagException{
    public FlagNotFoundException(Long flagId) {
        super(FlagErrorCode.FLAG_NOT_FOUND, "존재하지 않는 flag : " + flagId);
    }
}
