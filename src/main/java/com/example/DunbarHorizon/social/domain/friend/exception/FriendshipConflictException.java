package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;

public class FriendshipConflictException extends FriendException {

    public FriendshipConflictException(Long lowUserId, Long highUserId) {
        super(GlobalErrorCode.CONCURRENT_MODIFICATION,
                ErrorContext.of("lowUserId", lowUserId).and("highUserId", highUserId));
    }
}
