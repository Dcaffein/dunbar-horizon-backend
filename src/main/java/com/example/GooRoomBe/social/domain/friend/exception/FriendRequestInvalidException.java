package com.example.GooRoomBe.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendRequestInvalidException extends FriendException {
    public FriendRequestInvalidException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
