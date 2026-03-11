package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AccountException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
