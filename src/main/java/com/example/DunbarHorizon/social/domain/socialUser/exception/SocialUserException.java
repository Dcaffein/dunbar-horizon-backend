package com.example.DunbarHorizon.social.domain.socialUser.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public abstract class SocialUserException extends BusinessException {
    protected SocialUserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
