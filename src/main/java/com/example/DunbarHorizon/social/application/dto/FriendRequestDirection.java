package com.example.DunbarHorizon.social.application.dto;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;

import java.util.Locale;

public enum FriendRequestDirection {
    RECEIVED,
    SENT;

    public static FriendRequestDirection from(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_INVALID_DIRECTION,
                    ErrorContext.of("direction", value));
        }
    }
}
