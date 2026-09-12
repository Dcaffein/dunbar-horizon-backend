package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class DuplicateLabelMemberException extends LabelException {

    public DuplicateLabelMemberException(Long memberId) {
        super(SocialErrorCode.LABEL_MEMBER_DUPLICATE, ErrorContext.of("memberId", memberId));
    }
}
