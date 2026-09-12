package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendRequestNotFoundException extends FriendException {

    public FriendRequestNotFoundException(String requestId) {
        super(SocialErrorCode.FRIEND_REQUEST_NOT_FOUND, ErrorContext.of("requestId", requestId));
    }
}
