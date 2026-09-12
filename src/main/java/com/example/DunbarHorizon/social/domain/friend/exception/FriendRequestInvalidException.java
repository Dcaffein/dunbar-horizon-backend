package com.example.DunbarHorizon.social.domain.friend.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;



/**
 * 조건마다 코드가 다르다. 호출부가 SocialErrorCode의 상수를 지정한다.
 */
public class FriendRequestInvalidException extends FriendException {

    public FriendRequestInvalidException(SocialErrorCode errorCode) {
        super(errorCode);
    }

    public FriendRequestInvalidException(SocialErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
