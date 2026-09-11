package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class DuplicateLabelMemberException extends LabelException {
    public DuplicateLabelMemberException(Long memberId) {
        super(SocialErrorCode.LABEL_MEMBER_DUPLICATE, String.format("User(%s)는 이미 라벨 멤버입니다.", memberId));
    }
}
