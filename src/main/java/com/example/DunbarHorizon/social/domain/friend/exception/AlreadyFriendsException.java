package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class AlreadyFriendsException extends FriendException {

    public AlreadyFriendsException(Long user1Id, Long user2Id) {
        super(SocialErrorCode.FRIEND_ALREADY_CONNECTED, ErrorContext.of("user1Id", user1Id).and("user2Id", user2Id));
    }
}
