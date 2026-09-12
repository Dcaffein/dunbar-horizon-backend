package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class LabelNotFoundException extends LabelException {

    public LabelNotFoundException(String labelId) {
        super(SocialErrorCode.LABEL_NOT_FOUND, ErrorContext.of("labelId", labelId));
    }
}
