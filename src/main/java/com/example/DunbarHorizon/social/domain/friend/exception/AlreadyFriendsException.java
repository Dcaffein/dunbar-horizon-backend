package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class AlreadyFriendsException extends FriendException {
    public AlreadyFriendsException(Long user1Id, Long user2Id) {
        super(SocialErrorCode.FRIEND_ALREADY_CONNECTED, String.format("User(%s)와 User(%s)는 이미 친구 관계입니다.", user1Id, user2Id));
    }
}
