package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendRequestNotAcceptedException extends FriendException {

    public FriendRequestNotAcceptedException(Long requesterId, Long receiverId) {
        super(SocialErrorCode.FRIEND_REQUEST_NOT_ACCEPTED, ErrorContext.of("requesterId", requesterId)
                .and("receiverId", receiverId));
    }
}
