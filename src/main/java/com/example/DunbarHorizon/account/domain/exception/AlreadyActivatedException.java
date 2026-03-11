package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class AlreadyActivatedException extends AccountException{
    public AlreadyActivatedException(Long userId) {
        super("not unverified user : " + userId, HttpStatus.CONFLICT);
    }
}
