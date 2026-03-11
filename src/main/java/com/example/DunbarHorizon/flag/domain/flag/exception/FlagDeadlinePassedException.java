package com.example.DunbarHorizon.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagDeadlinePassedException extends FlagException {
    public FlagDeadlinePassedException() {
        super("모집 기간이 지난 깃발입니다.", HttpStatus.CONFLICT);
    }
}
