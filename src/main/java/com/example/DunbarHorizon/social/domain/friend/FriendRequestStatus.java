package com.example.DunbarHorizon.social.domain.friend;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;

public enum FriendRequestStatus {

    PENDING {
        @Override
        public FriendRequestStatus update(FriendRequest request, Long userId) {
            if (request.getStatus() != HIDDEN) {
                return throwTransitionInvalid(request.getStatus(), PENDING);
            }
            validateReceiver(request, userId);
            return this;
        }

        @Override
        public void cancel(FriendRequest request, Long userId) {
            validateRequester(request, userId);
        }
    },

    ACCEPTED {
        @Override
        public FriendRequestStatus update(FriendRequest request, Long userId) {
            if (request.getStatus() != PENDING && request.getStatus() != HIDDEN) {
                return throwTransitionInvalid(request.getStatus(), ACCEPTED);
            }
            validateReceiver(request, userId);
            return this;
        }
    },

    HIDDEN {
        @Override
        public FriendRequestStatus update(FriendRequest request, Long userId) {
            if (request.getStatus() != PENDING) {
                return throwTransitionInvalid(request.getStatus(), HIDDEN);
            }
            validateReceiver(request, userId);
            return this;
        }
    };

    public FriendRequestStatus update(FriendRequest request, Long userId) {
        return throwTransitionInvalid(request.getStatus(), this);
    }

    public void cancel(FriendRequest request, Long userId) {
        throwCancelNotAllowed(this);
    }

    protected void validateReceiver(FriendRequest request, Long userId) {
        if (!request.getReceiver().getId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getId(), userId);
        }
    }

    protected void validateRequester(FriendRequest request, Long userId) {
        if (!request.getRequester().getId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getId(), userId);
        }
    }

    private static FriendRequestStatus throwTransitionInvalid(FriendRequestStatus from, FriendRequestStatus to) {
        throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_TRANSITION_INVALID,
                ErrorContext.of("from", from).and("to", to));
    }

    private static void throwCancelNotAllowed(FriendRequestStatus status) {
        throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_CANCEL_NOT_ALLOWED,
                ErrorContext.of("status", status));
    }
}
