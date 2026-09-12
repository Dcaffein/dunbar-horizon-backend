package com.example.DunbarHorizon.social.domain.socialUser.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public abstract class SocialUserException extends BusinessException {

    protected SocialUserException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected SocialUserException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
