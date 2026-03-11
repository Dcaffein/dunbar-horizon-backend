package com.example.DunbarHorizon.social.domain.socialUser.exception;

import org.springframework.http.HttpStatus;

public class UserReferenceNotFoundException extends SocialUserException {
    public UserReferenceNotFoundException(Long userId) {
        super(String.format("User(%s)를 찾을 수 없습니다.", userId), HttpStatus.NOT_FOUND);
    }
}
