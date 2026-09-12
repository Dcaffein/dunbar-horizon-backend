package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class CannotRequestToSelfException extends FriendException {

    public CannotRequestToSelfException(Long userId) {
        super(SocialErrorCode.FRIEND_REQUEST_TO_SELF, ErrorContext.of("userId", userId));
    }
}
