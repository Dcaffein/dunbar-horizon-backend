package com.example.DunbarHorizon.social.domain.friendship.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendRequestNotFoundException extends FriendException {

    public FriendRequestNotFoundException(Long userId, Long counterpartId) {
        super(SocialErrorCode.FRIEND_REQUEST_NOT_FOUND, ErrorContext.of("userId", userId).and("counterpartId", counterpartId));
    }
}
