package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;

public class FriendRequestConflictException extends FriendException {

    public FriendRequestConflictException(Long requesterId, Long receiverId) {
        super(GlobalErrorCode.CONCURRENT_MODIFICATION,
                ErrorContext.of("requesterId", requesterId).and("receiverId", receiverId));
    }
}
