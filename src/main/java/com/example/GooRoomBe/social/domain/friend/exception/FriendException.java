package com.example.GooRoomBe.social.domain.friend.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class FriendException extends BusinessException {
    protected FriendException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
