package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class DuplicateLabelNameException extends LabelException {
    public DuplicateLabelNameException(Long ownerId, String labelName) {
        super(SocialErrorCode.LABEL_NAME_DUPLICATE, String.format("User(%s)는 이미 '%s'라는 이름의 라벨을 가지고 있습니다.", ownerId, labelName));
    }
}
