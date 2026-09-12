package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;


public class InvalidLabelNameException extends LabelException {

    public InvalidLabelNameException() {
        super(SocialErrorCode.LABEL_INVALID_NAME);
    }
}
