package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;
import java.util.List;


public class NonFriendLabelMemberException extends LabelException {

    public NonFriendLabelMemberException(List<Long> nonFriendIds) {
        super(SocialErrorCode.LABEL_MEMBER_NOT_FRIEND, ErrorContext.of("nonFriendIds", nonFriendIds));
    }
}
