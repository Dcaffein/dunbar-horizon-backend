package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class DuplicateFriendRequestException extends FriendException {

    public DuplicateFriendRequestException(Long requesterId, Long receiverId) {
        super(SocialErrorCode.FRIEND_REQUEST_DUPLICATE, ErrorContext.of("requesterId", requesterId).and("receiverId", receiverId));
    }
}
