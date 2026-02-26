package com.example.GooRoomBe.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendshipNotFoundException extends FriendException {
    public FriendshipNotFoundException(Long user1Id, Long user2Id) {
        super(String.format("User(%s)와 User(%s) 사이의 친구 관계를 찾을 수 없습니다.", user1Id, user2Id), HttpStatus.NOT_FOUND);
    }
}