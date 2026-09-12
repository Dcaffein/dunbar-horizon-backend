package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendshipInvalidException extends FriendException {

    public FriendshipInvalidException(String friendshipId) {
        super(SocialErrorCode.FRIENDSHIP_MEMBER_COUNT_INVALID, ErrorContext.of("friendshipId", friendshipId));
    }
}
