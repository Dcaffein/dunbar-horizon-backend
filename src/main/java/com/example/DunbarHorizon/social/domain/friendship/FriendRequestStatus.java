package com.example.DunbarHorizon.social.domain.friendship;



import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendRequestAuthorizationException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendRequestInvalidException;

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

        @Override
        public void cancel(FriendRequest request, Long userId) {
            validateRequester(request, userId);
        }
    };

    public FriendRequestStatus update(FriendRequest request, Long userId) {
        return throwTransitionInvalid(request.getStatus(), this);
    }

    public void cancel(FriendRequest request, Long userId) {
        throwCancelNotAllowed(this);
    }

    public FriendRequestStatus visibleTo(Long requesterId, Long viewerId) {
        if (this == HIDDEN && requesterId.equals(viewerId)) {
            return PENDING;
        }
        return this;
    }

    protected void validateReceiver(FriendRequest request, Long userId) {
        if (!request.getReceiverId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getRequesterId(), request.getReceiverId(), userId);
        }
    }

    protected void validateRequester(FriendRequest request, Long userId) {
        if (!request.getRequesterId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getRequesterId(), request.getReceiverId(), userId);
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
