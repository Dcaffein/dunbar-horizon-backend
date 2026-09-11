package com.example.DunbarHorizon.social.domain.socialUser.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class UserReferenceNotFoundException extends SocialUserException {
    public UserReferenceNotFoundException(Long userId) {
        super(SocialErrorCode.SOCIAL_USER_NOT_FOUND, String.format("User(%s)를 찾을 수 없습니다.", userId));
    }
}
