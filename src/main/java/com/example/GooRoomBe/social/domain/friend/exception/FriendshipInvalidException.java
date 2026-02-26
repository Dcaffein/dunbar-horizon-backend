package com.example.GooRoomBe.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class FriendshipInvalidException extends FriendException {
    public FriendshipInvalidException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
