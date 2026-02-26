package com.example.GooRoomBe.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class LabelNotFoundException extends LabelException {
    public LabelNotFoundException(String labelId) {
        super("해당 id를 가진 label을 찾을 수 없습니다 : " + labelId, HttpStatus.NOT_FOUND);
    }
}
