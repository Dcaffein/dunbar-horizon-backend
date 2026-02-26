package com.example.GooRoomBe.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class DuplicateLabelMemberException extends LabelException {
    public DuplicateLabelMemberException(Long memberId) {
        super(String.format("User(%s)는 이미 라벨 멤버입니다.", memberId), HttpStatus.CONFLICT);
    }
}
