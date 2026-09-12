package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendRequestAuthorizationException extends FriendException {

    public FriendRequestAuthorizationException(String requestId, Long userId) {
        super(SocialErrorCode.FRIEND_REQUEST_ACCESS_DENIED, ErrorContext.of("requestId", requestId).and("userId", userId));
    }
}
