package com.example.DunbarHorizon.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class LabelAuthorizationException extends LabelException {
    public LabelAuthorizationException(Long userId) {
        super(String.format("User(%s)는 해당 Label에 대한 권한이 없습니다.", userId), HttpStatus.FORBIDDEN);
    }
}