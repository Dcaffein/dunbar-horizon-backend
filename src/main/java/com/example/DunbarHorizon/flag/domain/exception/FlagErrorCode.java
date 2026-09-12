package com.example.DunbarHorizon.flag.domain.exception;

import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * flag 도메인의 에러 계약. 조건 하나에 코드 하나다.
 *
 * <p>길이 위반 문구는 도메인 상수를 참조한다. 값을 여기서 다시 쓰면 DTO·도메인·에러 계약이
 * 서로 다른 문장을 말하게 된다.
 */
@RequiredArgsConstructor
public enum FlagErrorCode implements ErrorCode {

    // 조회
    FLAG_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 깃발을 찾을 수 없습니다."),
    FLAG_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 정보를 찾을 수 없습니다."),

    // 입력
    FLAG_TITLE_LENGTH_INVALID(HttpStatus.BAD_REQUEST, Flag.TITLE_LENGTH_MESSAGE),
    FLAG_DESCRIPTION_LENGTH_INVALID(HttpStatus.BAD_REQUEST, Flag.DESCRIPTION_LENGTH_MESSAGE),
    FLAG_INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "인원 제한은 최소 1명 이상이어야 합니다."),
    FLAG_HOST_REQUIRED(HttpStatus.BAD_REQUEST, "호스트 정보는 필수입니다."),
    FLAG_SCHEDULE_REQUIRED(HttpStatus.BAD_REQUEST, "시작과 종료 시간은 필수입니다."),
    FLAG_SCHEDULE_END_BEFORE_START(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 늦어야 합니다."),
    FLAG_SCHEDULE_DEADLINE_AFTER_START(HttpStatus.BAD_REQUEST, "모집 마감은 시작 시간보다 빨라야 합니다."),

    // 상태
    FLAG_NOT_RECRUITING(HttpStatus.CONFLICT, "모집 중인 깃발이 아닙니다."),
    FLAG_RECRUITMENT_CLOSED(HttpStatus.CONFLICT, "모집이 마감되어 참여를 취소할 수 없습니다."),
    FLAG_DEADLINE_PASSED(HttpStatus.CONFLICT, "모집 기간이 지난 깃발입니다."),
    FLAG_FULL_CAPACITY(HttpStatus.CONFLICT, "정원이 가득 찬 깃발입니다."),
    FLAG_PARTICIPATION_DUPLICATE(HttpStatus.CONFLICT, "이미 참여한 깃발입니다."),
    FLAG_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작된 깃발은 일정을 바꿀 수 없습니다."),
    FLAG_ALREADY_ENDED(HttpStatus.CONFLICT, "종료된 깃발은 수정할 수 없습니다."),
    FLAG_NOT_ENDED(HttpStatus.CONFLICT, "아직 종료되지 않은 깃발입니다."),
    FLAG_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 깃발입니다."),
    FLAG_ENCORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 앵콜이 만들어진 깃발입니다."),
    FLAG_CAPACITY_BELOW_PARTICIPANTS(HttpStatus.CONFLICT, "현재 참여 인원보다 적은 수로 정원을 바꿀 수 없습니다."),

    // 권한
    FLAG_HOST_ONLY(HttpStatus.FORBIDDEN, "호스트만 할 수 있는 작업입니다."),
    FLAG_PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "깃발 참여자만 볼 수 있습니다."),
    FLAG_SELF_PARTICIPATION_ONLY(HttpStatus.FORBIDDEN, "본인의 참여만 취소할 수 있습니다."),
    FLAG_HOST_NOT_ELIGIBLE(HttpStatus.FORBIDDEN, "호스트는 참여자나 초대 대상이 될 수 없습니다."),
    FLAG_FRIENDS_ONLY(HttpStatus.FORBIDDEN, "호스트의 친구만 참여할 수 있는 깃발입니다."),
    FLAG_INVITE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "초대 권한이 없습니다."),

    // 댓글
    FLAG_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    FLAG_COMMENT_INVALID_CONTENT(HttpStatus.BAD_REQUEST, FlagComment.CONTENT_LENGTH_MESSAGE),
    FLAG_COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),
    FLAG_COMMENT_AUTHOR_ONLY(HttpStatus.FORBIDDEN, "작성자만 할 수 있는 작업입니다."),

    // 초대
    FLAG_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대장을 찾을 수 없습니다."),
    FLAG_INVITATION_DUPLICATE(HttpStatus.CONFLICT, "이미 보낸 초대장이 있습니다."),
    FLAG_INVITATION_INVALID_DIRECTION(HttpStatus.BAD_REQUEST, "direction은 received 또는 sent만 사용할 수 있습니다."),
    FLAG_INVITATION_SENDER_ONLY(HttpStatus.FORBIDDEN, "초대를 보낸 본인만 취소할 수 있습니다."),
    FLAG_INVITATION_INVITEE_ONLY(HttpStatus.FORBIDDEN, "초대받은 본인만 응답할 수 있습니다."),

    // 후기
    FLAG_MEMORIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."),
    FLAG_MEMORIAL_INVALID_CONTENT(HttpStatus.BAD_REQUEST, FlagMemorial.CONTENT_LENGTH_MESSAGE),
    FLAG_MEMORIAL_PARTICIPANT_ONLY(HttpStatus.FORBIDDEN, "깃발 참여자만 후기를 작성할 수 있습니다."),
    FLAG_MEMORIAL_AUTHOR_ONLY(HttpStatus.FORBIDDEN, "후기 작성자만 접근할 수 있습니다.");

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
