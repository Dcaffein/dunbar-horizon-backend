package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class LabelAuthorizationException extends LabelException {

    public LabelAuthorizationException(Long userId) {
        super(SocialErrorCode.LABEL_ACCESS_DENIED, ErrorContext.of("userId", userId));
    }
}
