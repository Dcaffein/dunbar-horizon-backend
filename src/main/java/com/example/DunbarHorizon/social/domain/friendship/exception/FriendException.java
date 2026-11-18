package com.example.DunbarHorizon.social.domain.friendship.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class FriendException extends BusinessException {

    protected FriendException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected FriendException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
