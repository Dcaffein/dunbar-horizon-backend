package com.example.GooRoomBe.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class InvalidLabelNameException extends LabelException {
    public InvalidLabelNameException() {
        super("라벨 이름은 공백일 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}