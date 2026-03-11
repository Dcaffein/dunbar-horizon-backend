package com.example.DunbarHorizon.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class AlreadyFriendsException extends FriendException {
    public AlreadyFriendsException(Long user1Id, Long user2Id) {
        super(String.format("User(%s)와 User(%s)는 이미 친구 관계입니다.", user1Id, user2Id), HttpStatus.CONFLICT);
    }
}
