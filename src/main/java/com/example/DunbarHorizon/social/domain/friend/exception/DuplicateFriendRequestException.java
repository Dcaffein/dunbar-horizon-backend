package com.example.DunbarHorizon.social.domain.friend.exception;

import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;

public class DuplicateFriendRequestException extends FriendException {
    public DuplicateFriendRequestException(Long requesterId, Long receiverId) {
        super(SocialErrorCode.FRIEND_REQUEST_DUPLICATE, String.format("이미 친구 요청이 존재합니다. (Sender: %s, Receiver: %s)", requesterId, receiverId));
    }
}
