package com.example.DunbarHorizon.flag.domain.memorial.exception;

import org.springframework.http.HttpStatus;

public class FlagMemorialNotFoundException extends FlagMemorialException {
    public FlagMemorialNotFoundException(Long flagMemorialId) {
        super("존재하지 않는 flagMemorial : " + flagMemorialId, HttpStatus.NOT_FOUND);
    }
}
