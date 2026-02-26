package com.example.GooRoomBe.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendshipAuthorizationException extends FriendException {
    public FriendshipAuthorizationException(Long userId) {
        super(String.format("User(%s)는 해당 친구 관계를 관리할 권한이 없습니다.", userId), HttpStatus.FORBIDDEN);
    }
}