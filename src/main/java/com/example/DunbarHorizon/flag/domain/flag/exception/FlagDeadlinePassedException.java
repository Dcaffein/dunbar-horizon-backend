package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import java.time.LocalDateTime;


public class FlagDeadlinePassedException extends FlagException {

    public FlagDeadlinePassedException(Long flagId, LocalDateTime deadline) {
        super(FlagErrorCode.FLAG_DEADLINE_PASSED, ErrorContext.of("flagId", flagId).and("deadline", deadline));
    }
}
