package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum BuzzErrorCode implements ErrorCode {

    BUZZ_NOT_FOUND(HttpStatus.NOT_FOUND, "버즈를 찾을 수 없습니다."),
    BUZZ_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),

    BUZZ_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 버즈입니다."),
    BUZZ_INVALID_TEXT(HttpStatus.BAD_REQUEST, Buzz.TEXT_LENGTH_MESSAGE),
    BUZZ_COMMENT_INVALID_TEXT(HttpStatus.BAD_REQUEST, BuzzComment.TEXT_LENGTH_MESSAGE),
    BUZZ_RECIPIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "수신자를 한 명 이상 지정해주세요."),
    BUZZ_RECIPIENTS_EXCEEDED(HttpStatus.BAD_REQUEST, "수신자는 최대 150명까지 지정할 수 있습니다."),

    BUZZ_NOT_RECIPIENT(HttpStatus.FORBIDDEN, "이 버즈의 수신자가 아닙니다."),
    BUZZ_CREATOR_ONLY(HttpStatus.FORBIDDEN, "작성자만 할 수 있는 작업입니다."),
    BUZZ_COMMENT_AUTHOR_ONLY(HttpStatus.FORBIDDEN, "댓글 작성자만 할 수 있는 작업입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
