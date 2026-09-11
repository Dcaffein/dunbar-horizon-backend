package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class CannotRequestToSelfException extends FriendException {
    public CannotRequestToSelfException(Long userId) {
        super(SocialErrorCode.FRIEND_REQUEST_TO_SELF, String.format("자기 자신(ID: %s)에게는 친구 요청을 보낼 수 없습니다.", userId));
    }
}