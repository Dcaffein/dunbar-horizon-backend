package com.example.DunbarHorizon.social.domain.label.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

import java.util.List;

public class NonFriendLabelMemberException extends LabelException {
    public NonFriendLabelMemberException(List<Long> nonFriendIds) {
        super(SocialErrorCode.LABEL_MEMBER_NOT_FRIEND, String.format("다음 사용자들은 친구 관계가 아니므로 멤버로 추가할 수 없습니다: %s", nonFriendIds));
    }
}
