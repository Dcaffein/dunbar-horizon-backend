package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendRequestNotAcceptedException extends FriendException {
    public FriendRequestNotAcceptedException(String requestId) {
        super(SocialErrorCode.FRIEND_REQUEST_NOT_ACCEPTED, String.format("친구 요청(ID: %s)은 수락(ACCEPTED) 상태가 아닙니다.", requestId));
    }
}
