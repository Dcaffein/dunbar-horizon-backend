package com.example.DunbarHorizon.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class CannotRequestToSelfException extends FriendException {
    public CannotRequestToSelfException(Long userId) {
        super(String.format("자기 자신(ID: %s)에게는 친구 요청을 보낼 수 없습니다.", userId), HttpStatus.BAD_REQUEST);
    }
}