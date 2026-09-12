package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;


public class FlagParticipationDuplicateException extends FlagException {

    public FlagParticipationDuplicateException(Long flagId, Long userId) {
        super(FlagErrorCode.FLAG_PARTICIPATION_DUPLICATE, ErrorContext.of("flagId", flagId).and("userId", userId));
    }
}
