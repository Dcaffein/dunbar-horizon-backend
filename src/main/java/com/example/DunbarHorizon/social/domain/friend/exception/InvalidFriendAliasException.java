package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class InvalidFriendAliasException extends FriendException {
    public InvalidFriendAliasException(String message) {
        super(SocialErrorCode.FRIENDSHIP_INVALID_ALIAS, message);
    }
}
