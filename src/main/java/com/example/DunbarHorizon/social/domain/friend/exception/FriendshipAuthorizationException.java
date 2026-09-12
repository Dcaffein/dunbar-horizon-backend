package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendshipAuthorizationException extends FriendException {

    public FriendshipAuthorizationException(Long userId) {
        super(SocialErrorCode.FRIENDSHIP_ACCESS_DENIED, ErrorContext.of("userId", userId));
    }
}
