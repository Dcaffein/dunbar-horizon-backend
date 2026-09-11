package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendshipAuthorizationException extends FriendException {
    public FriendshipAuthorizationException(Long userId) {
        super(SocialErrorCode.FRIENDSHIP_ACCESS_DENIED, String.format("User(%s)는 해당 친구 관계를 관리할 권한이 없습니다.", userId));
    }
}