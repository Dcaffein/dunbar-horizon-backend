package com.example.DunbarHorizon.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagNotFoundException extends FlagException{
    public FlagNotFoundException(Long flagId) {
        super("존재하지 않는 flag : " + flagId, HttpStatus.NOT_FOUND);
    }
}
