package com.example.GooRoomBe.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendRequestAuthorizationException extends FriendException {
    public FriendRequestAuthorizationException(String requestId, Long userId) {
        super(String.format("User(ID: %s)는 친구 요청(ID: %s)을 처리할 권한이 없습니다.", userId, requestId), HttpStatus.FORBIDDEN);
    }
}