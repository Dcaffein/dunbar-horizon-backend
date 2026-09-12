package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class DuplicateLabelNameException extends LabelException {

    public DuplicateLabelNameException(Long ownerId, String labelName) {
        super(SocialErrorCode.LABEL_NAME_DUPLICATE, ErrorContext.of("ownerId", ownerId).and("labelName", labelName));
    }
}
