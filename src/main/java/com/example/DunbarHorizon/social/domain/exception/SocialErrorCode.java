package com.example.DunbarHorizon.social.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorCode;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.label.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SocialErrorCode implements ErrorCode {

    // 친구 요청
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."),
    FRIEND_REQUEST_DUPLICATE(HttpStatus.CONFLICT, "이미 보낸 친구 요청이 있습니다."),
    FRIEND_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIEND_REQUEST_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "수락되지 않은 친구 요청입니다."),
    FRIEND_REQUEST_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "변경할 상태는 필수입니다."),
    FRIEND_REQUEST_STATUS_TRANSITION_INVALID(HttpStatus.BAD_REQUEST, "현재 상태에서는 변경할 수 없습니다."),
    FRIEND_REQUEST_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 상태에서는 취소할 수 없습니다."),
    FRIEND_REQUEST_STATUS_NOT_ALLOWED_FOR_SENT(HttpStatus.BAD_REQUEST, "보낸 요청 조회에는 상태를 지정할 수 없습니다."),
    FRIEND_REQUEST_INVALID_DIRECTION(HttpStatus.BAD_REQUEST, "direction은 received 또는 sent만 사용할 수 있습니다."),
    FRIEND_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 친구 요청을 처리할 권한이 없습니다."),

    // 친구 관계
    FRIEND_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 친구입니다."),
    FRIENDSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 관계를 찾을 수 없습니다."),
    FRIENDSHIP_MEMBER_COUNT_INVALID(HttpStatus.CONFLICT, "친구 관계 정보가 올바르지 않습니다."),
    FRIENDSHIP_INVALID_ALIAS(HttpStatus.BAD_REQUEST, Friendship.ALIAS_LENGTH_MESSAGE),
    FRIENDSHIP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 친구 관계를 관리할 권한이 없습니다."),

    // 라벨
    LABEL_NOT_FOUND(HttpStatus.NOT_FOUND, "라벨을 찾을 수 없습니다."),
    LABEL_NAME_DUPLICATE(HttpStatus.CONFLICT, "같은 이름의 라벨이 이미 있습니다."),
    LABEL_MEMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 라벨에 포함된 멤버입니다."),
    LABEL_INVALID_NAME(HttpStatus.BAD_REQUEST, Label.NAME_LENGTH_MESSAGE),
    LABEL_MEMBER_NOT_FRIEND(HttpStatus.BAD_REQUEST, "친구가 아닌 사용자는 라벨에 추가할 수 없습니다."),
    LABEL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 라벨에 대한 권한이 없습니다."),

    // 사용자 참조
    SOCIAL_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");

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
