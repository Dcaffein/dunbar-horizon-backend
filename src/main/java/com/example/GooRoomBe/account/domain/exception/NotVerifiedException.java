package com.example.GooRoomBe.account.domain.exception;

import org.springframework.http.HttpStatus;

public class NotVerifiedException extends AccountException{
    public NotVerifiedException(Long authId) {
        super("not verified auth : " + authId, HttpStatus.CONFLICT);
    }
}
