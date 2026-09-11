package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendRequestNotFoundException extends FriendException {
    public FriendRequestNotFoundException(String requestId) {
        super(SocialErrorCode.FRIEND_REQUEST_NOT_FOUND, String.format("해당 친구 요청(ID: %s)을 찾을 수 없습니다.", requestId));
    }
}