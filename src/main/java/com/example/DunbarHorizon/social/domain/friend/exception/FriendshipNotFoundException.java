package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class FriendshipNotFoundException extends FriendException {
    public FriendshipNotFoundException(Long user1Id, Long user2Id) {
        super(SocialErrorCode.FRIENDSHIP_NOT_FOUND, String.format("User(%s)와 User(%s) 사이의 친구 관계를 찾을 수 없습니다.", user1Id, user2Id));
    }
}