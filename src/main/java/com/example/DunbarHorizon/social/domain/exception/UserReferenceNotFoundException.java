package com.example.DunbarHorizon.social.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class UserReferenceNotFoundException extends SocialUserException {

    public UserReferenceNotFoundException(Long userId) {
        super(SocialErrorCode.SOCIAL_USER_NOT_FOUND, ErrorContext.of("userId", userId));
    }
}
