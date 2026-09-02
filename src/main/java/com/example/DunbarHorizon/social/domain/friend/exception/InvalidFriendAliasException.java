package com.example.DunbarHorizon.social.domain.friend.exception;

import org.springframework.http.HttpStatus;

public class InvalidFriendAliasException extends FriendException {
    public InvalidFriendAliasException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
