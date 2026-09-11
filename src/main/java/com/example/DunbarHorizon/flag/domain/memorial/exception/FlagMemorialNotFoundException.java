package com.example.DunbarHorizon.flag.domain.memorial.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagMemorialNotFoundException extends FlagMemorialException {
    public FlagMemorialNotFoundException(Long flagMemorialId) {
        super(FlagErrorCode.FLAG_MEMORIAL_NOT_FOUND, "존재하지 않는 flagMemorial : " + flagMemorialId);
    }
}
