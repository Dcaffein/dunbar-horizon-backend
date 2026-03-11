package com.example.DunbarHorizon.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class DuplicateFriendRequestException extends FriendException {
    public DuplicateFriendRequestException(Long requesterId, Long receiverId) {
        super(String.format("이미 친구 요청이 존재합니다. (Sender: %s, Receiver: %s)", requesterId, receiverId), HttpStatus.CONFLICT);
    }
}
