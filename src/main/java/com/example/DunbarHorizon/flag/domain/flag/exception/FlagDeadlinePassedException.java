package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagDeadlinePassedException extends FlagException {
    public FlagDeadlinePassedException() {
        super(FlagErrorCode.FLAG_DEADLINE_PASSED, "모집 기간이 지난 깃발입니다.");
    }
}
