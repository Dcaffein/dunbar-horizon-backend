package com.example.DunbarHorizon.social.domain.friendship.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class FriendRequestAuthorizationException extends FriendException {

    public FriendRequestAuthorizationException(Long requesterId, Long receiverId, Long userId) {
        super(SocialErrorCode.FRIEND_REQUEST_ACCESS_DENIED, ErrorContext.of("requesterId", requesterId)
                .and("receiverId", receiverId).and("userId", userId));
    }
}
