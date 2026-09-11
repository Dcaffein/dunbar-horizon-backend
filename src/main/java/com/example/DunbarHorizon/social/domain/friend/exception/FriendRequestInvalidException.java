package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendRequestInvalidException extends FriendException {
    public FriendRequestInvalidException(String message) {
        super(SocialErrorCode.FRIEND_REQUEST_INVALID, message);
    }
}
