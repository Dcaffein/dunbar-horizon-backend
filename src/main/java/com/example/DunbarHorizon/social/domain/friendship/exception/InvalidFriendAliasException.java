package com.example.DunbarHorizon.social.domain.friendship.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class InvalidFriendAliasException extends FriendException {

    public InvalidFriendAliasException() {
        super(SocialErrorCode.FRIENDSHIP_INVALID_ALIAS);
    }
}
