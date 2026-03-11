package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class FriendException extends BusinessException {
    protected FriendException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
