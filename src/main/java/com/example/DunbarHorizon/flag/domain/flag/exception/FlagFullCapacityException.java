package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagFullCapacityException extends FlagException {
    public FlagFullCapacityException() {
        super(FlagErrorCode.FLAG_FULL_CAPACITY, "정원이 가득 찬 깃발입니다.");
    }
}
