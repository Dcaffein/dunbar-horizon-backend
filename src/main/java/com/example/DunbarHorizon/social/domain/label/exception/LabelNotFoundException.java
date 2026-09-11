package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class LabelNotFoundException extends LabelException {
    public LabelNotFoundException(String labelId) {
        super(SocialErrorCode.LABEL_NOT_FOUND, "해당 id를 가진 label을 찾을 수 없습니다 : " + labelId);
    }
}
