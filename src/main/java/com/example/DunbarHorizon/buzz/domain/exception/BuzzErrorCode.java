package com.example.DunbarHorizon.buzz.domain.exception;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum BuzzErrorCode implements ErrorCode {

    BUZZ_NOT_FOUND(HttpStatus.NOT_FOUND, "버즈를 찾을 수 없습니다."),
    BUZZ_THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "스레드를 찾을 수 없습니다."),

    BUZZ_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 버즈입니다."),
    BUZZ_INVALID_TEXT(HttpStatus.BAD_REQUEST, Buzz.TEXT_LENGTH_MESSAGE),
    BUZZ_RECIPIENTS_REQUIRED(HttpStatus.BAD_REQUEST, "수신자를 한 명 이상 지정해주세요."),
    BUZZ_THREAD_INVALID_TEXT(HttpStatus.BAD_REQUEST, BuzzThread.TEXT_LENGTH_MESSAGE),

    BUZZ_NOT_RECIPIENT(HttpStatus.FORBIDDEN, "이 버즈의 수신자가 아닙니다."),
    BUZZ_CREATOR_ONLY(HttpStatus.FORBIDDEN, "작성자만 할 수 있는 작업입니다."),
    BUZZ_CREATOR_CANNOT_OPEN_THREAD(HttpStatus.FORBIDDEN, "게시자는 스레드를 열 수 없습니다. 답글로 참여해주세요."),
    BUZZ_THREAD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 스레드를 볼 수 없습니다."),

    BUZZ_THREAD_ALREADY_OPENED(HttpStatus.CONFLICT, "이미 같은 공개 범위의 스레드를 열었습니다.");

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
