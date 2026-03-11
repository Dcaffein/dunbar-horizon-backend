package com.example.DunbarHorizon.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendRequestNotFoundException extends FriendException {
    public FriendRequestNotFoundException(String requestId) {
        super(String.format("해당 친구 요청(ID: %s)을 찾을 수 없습니다.", requestId), HttpStatus.NOT_FOUND);
    }
}