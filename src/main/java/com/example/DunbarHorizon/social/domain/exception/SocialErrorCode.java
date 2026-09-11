package com.example.DunbarHorizon.social.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SocialErrorCode implements ErrorCode {

    FRIEND_ALREADY_CONNECTED(HttpStatus.CONFLICT),
    FRIEND_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_DUPLICATE(HttpStatus.CONFLICT),
    FRIEND_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FRIEND_REQUEST_INVALID(HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_ACCEPTED(HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND),

    FRIENDSHIP_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FRIENDSHIP_INVALID(HttpStatus.CONFLICT),
    FRIENDSHIP_INVALID_ALIAS(HttpStatus.BAD_REQUEST),
    FRIENDSHIP_NOT_FOUND(HttpStatus.NOT_FOUND),

    LABEL_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    LABEL_INVALID_NAME(HttpStatus.BAD_REQUEST),
    LABEL_MEMBER_DUPLICATE(HttpStatus.CONFLICT),
    LABEL_MEMBER_NOT_FRIEND(HttpStatus.BAD_REQUEST),
    LABEL_NAME_DUPLICATE(HttpStatus.CONFLICT),
    LABEL_NOT_FOUND(HttpStatus.NOT_FOUND),

    SOCIAL_USER_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus status;

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
