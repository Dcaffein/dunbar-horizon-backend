package com.example.DunbarHorizon.flag.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FlagErrorCode implements ErrorCode {

    FLAG_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FLAG_DEADLINE_PASSED(HttpStatus.CONFLICT),
    FLAG_FULL_CAPACITY(HttpStatus.CONFLICT),
    FLAG_INVALID_BASIC_INFO(HttpStatus.BAD_REQUEST),
    FLAG_INVALID_CAPACITY(HttpStatus.BAD_REQUEST),
    FLAG_INVALID_STATUS(HttpStatus.CONFLICT),
    FLAG_NOT_FOUND(HttpStatus.NOT_FOUND),
    FLAG_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND),
    FLAG_PARTICIPATION_DUPLICATE(HttpStatus.CONFLICT),
    FLAG_SCHEDULE_INVALID(HttpStatus.CONFLICT),

    FLAG_COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FLAG_COMMENT_INVALID_CONTENT(HttpStatus.BAD_REQUEST),
    FLAG_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    FLAG_COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST),

    FLAG_INVITATION_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FLAG_INVITATION_DUPLICATE(HttpStatus.CONFLICT),
    FLAG_INVITATION_INVALID(HttpStatus.BAD_REQUEST),
    FLAG_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND),

    FLAG_MEMORIAL_ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FLAG_MEMORIAL_INVALID_CONTENT(HttpStatus.BAD_REQUEST),
    FLAG_MEMORIAL_NOT_FOUND(HttpStatus.NOT_FOUND);

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
