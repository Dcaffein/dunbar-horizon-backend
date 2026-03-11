package com.example.DunbarHorizon.social.domain.socialUser.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class SocialUserException extends BusinessException {
    protected SocialUserException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
