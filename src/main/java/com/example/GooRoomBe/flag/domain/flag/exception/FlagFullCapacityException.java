package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagFullCapacityException extends FlagException {
    public FlagFullCapacityException() {
        super("정원이 가득 찬 깃발입니다.", HttpStatus.CONFLICT);
    }
}
