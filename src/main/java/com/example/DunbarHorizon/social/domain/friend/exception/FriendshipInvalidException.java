package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendshipInvalidException extends FriendException {
    public FriendshipInvalidException(String message) {
        super(SocialErrorCode.FRIENDSHIP_INVALID, message);
    }
}
