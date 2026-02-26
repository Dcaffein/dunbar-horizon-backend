package com.example.GooRoomBe.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class DuplicateLabelNameException extends LabelException {
    public DuplicateLabelNameException(Long ownerId, String labelName) {
        super(String.format("User(%s)는 이미 '%s'라는 이름의 라벨을 가지고 있습니다.", ownerId, labelName), HttpStatus.CONFLICT);
    }
}
