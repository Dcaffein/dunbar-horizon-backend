package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class LabelAuthorizationException extends LabelException {
    public LabelAuthorizationException(Long userId) {
        super(SocialErrorCode.LABEL_ACCESS_DENIED, String.format("User(%s)는 해당 Label에 대한 권한이 없습니다.", userId));
    }
}