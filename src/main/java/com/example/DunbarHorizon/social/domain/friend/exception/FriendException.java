package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class FriendException extends BusinessException {
    protected FriendException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
