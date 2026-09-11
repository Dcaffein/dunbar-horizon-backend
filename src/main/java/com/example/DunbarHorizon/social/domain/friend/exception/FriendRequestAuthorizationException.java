package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendRequestAuthorizationException extends FriendException {
    public FriendRequestAuthorizationException(String requestId, Long userId) {
        super(SocialErrorCode.FRIEND_REQUEST_ACCESS_DENIED, String.format("User(ID: %s)는 친구 요청(ID: %s)을 처리할 권한이 없습니다.", userId, requestId));
    }
}